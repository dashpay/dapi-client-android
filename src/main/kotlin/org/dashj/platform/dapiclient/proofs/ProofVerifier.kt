/**
 * Copyright (c) 2021-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.proofs

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.BLSSignature
import org.bitcoinj.evolution.SimplifiedMasternodeListManager
import org.bitcoinj.quorums.LLMQParameters
import org.bitcoinj.quorums.Quorum
import org.dashj.merk.ByteArrayKey
import org.dashj.merk.MerkVerifyProof
import org.dashj.merk.blake3
import org.dashj.platform.dapiclient.model.Proof
import org.dashj.platform.dapiclient.model.ResponseMetadata
import org.dashj.platform.dpp.hashOnce
import org.dashj.platform.dpp.hashTwice
import org.dashj.platform.dpp.toHex
import org.slf4j.LoggerFactory

object ProofVerifier {
    private val logger = LoggerFactory.getLogger(ProofVerifier::class.java.name)

    @JvmStatic
    fun verifyAndExtractFromProof(proof: Proof, metaData: ResponseMetadata, masternodeListManager: SimplifiedMasternodeListManager, caller: String): Map<Int, Map<ByteArrayKey, ByteArray>> {
        val quorum = masternodeListManager.quorumListAtTip.getQuorum(Sha256Hash.wrap(proof.signatureLlmqHash))
        return verifyAndExtractFromProof(proof, metaData, quorum, caller)
    }

    @JvmStatic
    fun verifyAndExtractFromProof(proof: Proof, metaData: ResponseMetadata, quorum: Quorum, caller: String): Map<Int, Map<ByteArrayKey, ByteArray>> {
        if (proof.signature.isEmpty()) {
            throw IllegalArgumentException("Platform returned an empty or wrongly sized signature")
        }

        // We first need to get the merk Root

        val proofs = proof.storeTreeProofs

        var identitiesPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null
        var documentsPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null
        var contractsPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null
        var publicKeystoIdentityIdsPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null

        val rootElementsToProve = hashMapOf<Int, ByteArray>()

        if (proofs.identitiesProof.isNotEmpty()) {
            identitiesPair = MerkVerifyProof.extractProofWithHash(proofs.identitiesProof)
            rootElementsToProve[Indices.Identities.value] = identitiesPair.first
        }

        if (proofs.documentsProof.isNotEmpty()) {
            documentsPair = MerkVerifyProof.extractProofWithHash(proofs.documentsProof)
            rootElementsToProve[Indices.Documents.value] = documentsPair.first
        }

        if (proofs.dataContractsProof.isNotEmpty()) {
            contractsPair = MerkVerifyProof.extractProofWithHash(proofs.dataContractsProof)
            rootElementsToProve[Indices.Contracts.value] = contractsPair.first
        }

        if (proofs.publicKeyHashesToIdentityIdsProof.isNotEmpty()) {
            publicKeystoIdentityIdsPair = MerkVerifyProof.extractProofWithHash(proofs.publicKeyHashesToIdentityIdsProof)
            rootElementsToProve[Indices.PublicKeyHashesToIdentityIds.value] = publicKeystoIdentityIdsPair.first
        }

        val merkleProof = MerkleProof.fromBuffer(proof.rootTreeProof) {
            blake3(it)
        }
        val stateHash = merkleProof.calculateRoot(rootElementsToProve.keys.toList(), rootElementsToProve.values.toList(), 6)

        val stateData = ByteArrayOutputStream(40)
        Utils.uint64ToByteStreamLE(BigInteger.valueOf(metaData.height - 1), stateData)
        stateData.write(stateHash)
        val stateMessageHash = stateData.toByteArray().hashOnce()

        val signatureVerified = verifyStateSignature(
            proof.signature,
            stateMessageHash,
            metaData.height - 1,
            quorum,
            quorum.llmqParameters.type
        )
        if (!signatureVerified) {
            val tryHigher = verifyStateSignature(
                proof.signature,
                stateMessageHash,
                metaData.height - 2,
                quorum,
                quorum.llmqParameters.type
            )
            val tryHigher2 = verifyStateSignature(
                proof.signature,
                stateMessageHash,
                metaData.height - 3,
                quorum,
                quorum.llmqParameters.type
            )
            logger.warn("verify(height=${metaData.height}, stateHash=${stateHash.toHex().substring(0, 20)}, quorum=${quorum.quorumHash.toString().substring(0, 20)}, $caller): unable to verify platform signature, $tryHigher, $tryHigher2")
        } else {
            logger.info("verify(height=${metaData.height}, stateHash=${stateHash.toHex().substring(0, 20)}, quorum=${quorum.quorumHash.toString().substring(0, 20)}, $caller): platform signature verified")
        }

        val elementsDictionary = hashMapOf<Int, Map<ByteArrayKey, ByteArray>>()
        if (identitiesPair != null) {
            elementsDictionary[Indices.Identities.value] = identitiesPair.second
        }
        if (documentsPair != null) {
            elementsDictionary[Indices.Documents.value] = documentsPair.second
        }
        if (contractsPair != null) {
            elementsDictionary[Indices.Contracts.value] = contractsPair.second
        }
        if (publicKeystoIdentityIdsPair != null) {
            elementsDictionary[Indices.PublicKeyHashesToIdentityIds.value] = publicKeystoIdentityIdsPair.second
        }

        return elementsDictionary
    }

    private fun requestIdForHeight(height: Long): ByteArray {
        val stream = ByteArrayOutputStream(15)
        stream.write("dpsvote".toByteArray(Charsets.UTF_8))
        Utils.uint64ToByteStreamLE(BigInteger.valueOf(height), stream)
        return stream.toByteArray().hashOnce()
    }

    private fun signIDForQuorumEntry(quorumEntry: Quorum, quorumType: LLMQParameters.LLMQType, stateMessageHash: ByteArray, height: Long): ByteArray {
        val requestId = requestIdForHeight(height)
        val stream = ByteArrayOutputStream(97)
        stream.write(quorumType.value)
        stream.write(quorumEntry.quorumHash.reversedBytes)
        stream.write(requestId.reversedArray())
        stream.write(stateMessageHash.reversedArray())
        return stream.toByteArray().hashTwice()
    }

    fun verifyStateSignature(signature: ByteArray, stateMessageHash: ByteArray, height: Long, quorumEntry: Quorum, quorumType: LLMQParameters.LLMQType): Boolean {
        val blsPublicKey = quorumEntry.commitment.quorumPublicKey

        val signId = signIDForQuorumEntry(quorumEntry, quorumType, stateMessageHash, height)
        val blsSignature = BLSSignature(signature)
        return blsSignature.verifyInsecure(blsPublicKey, Sha256Hash.wrap(signId))
    }
}
