package org.dashevo.dapiclient.model

import org.dashevo.dpp.contract.DataContractCreateTransition
import org.dashevo.dpp.document.DocumentCreateTransition
import org.dashevo.dpp.document.DocumentDeleteTransition
import org.dashevo.dpp.document.DocumentReplaceTransition
import org.dashevo.dpp.document.DocumentsBatchTransition
import org.dashevo.dpp.identifier.Identifier
import org.dashevo.dpp.identity.Identity
import org.dashevo.dpp.identity.IdentityCreateTransition
import org.dashevo.dpp.statetransition.StateTransitionIdentitySigned
import org.dashevo.dpp.util.Cbor
import org.dashj.merk.ByteArrayKey
import org.dashj.merk.MerkVerifyProof

open class DefaultVerifyProof(protected val stateTransition: StateTransitionIdentitySigned) : VerifyProof() {
    override fun verify(proof: Proof): Boolean {
        val values = MerkVerifyProof.decode(proof.storeTreeProof)
        val hash = MerkVerifyProof.decodeRootHash(proof.storeTreeProof)
        if (values.isNotEmpty()) {
            when (stateTransition) {
                is DocumentsBatchTransition -> {
                    return verifyDocumentsBatchTransition(stateTransition, values)
                }
                is DataContractCreateTransition -> {
                    return verifyDataContactCreateTransition(values[values.keys.first()]!!, stateTransition)
                }
                is IdentityCreateTransition -> {
                    return verifyIdentityCreateTransition(stateTransition, values[values.keys.first()]!!)
                }
            }
        }
        return false
    }

    private fun compareMaps(a: Map<String, Any?>, b: Map<String, Any?>): Boolean {
        val match = a.keys == b.keys
        if (!match) {
            return false
        } else {
            for (key in a.keys) {
                val equal = compareElements(a[key]!!, b[key]!!)
                if (!equal) {
                    println("$key doesn't match in $a and $b")
                    return false
                }
            }
        }
        return true
    }

    private fun compareElements(a: Any, b: Any): Boolean {
        return when (a) {
            is ByteArray -> {
                when (b) {
                    is ByteArray -> a.contentEquals(b)
                    is Identifier -> a.contentEquals(b.toBuffer())
                    else -> {
                        false
                    }
                }
            }
            else -> {
                a == b
            }
        }
    }

    protected fun verifyDocumentsBatchTransition(
        stateTransition: DocumentsBatchTransition,
        values: Map<ByteArrayKey, ByteArray>
    ): Boolean {
        for (transition in stateTransition.transitions) {
            val rawDocument = HashMap(transition.toObject())
            rawDocument.remove("\$action")
            rawDocument["\$protocolVersion"] = stateTransition.protocolVersion
            rawDocument["\$ownerId"] = stateTransition.ownerId
            val rawDocumentFromProofBytes = values[ByteArrayKey(transition.id.toBuffer())]
            if (rawDocumentFromProofBytes != null) {
                val rawDocumentFromProof = Cbor.decode(rawDocumentFromProofBytes)
                when (transition) {
                    is DocumentCreateTransition -> {
                        rawDocument.remove("\$entropy")
                        rawDocument["\$revision"] = 1
                    }
                    is DocumentReplaceTransition -> {
                        rawDocumentFromProof.remove("\$createdAt")
                    }
                    is DocumentDeleteTransition -> {

                    }
                }
                return compareMaps(rawDocumentFromProof, rawDocument)
            } else {
                return false
            }
        }
        return true
    }

    protected fun verifyDataContactCreateTransition(
        value: ByteArray,
        stateTransition: DataContractCreateTransition
    ): Boolean {
        val match = Cbor.decode(value) == stateTransition.dataContract.toObject()
        if (!match) {
            return false
        }
        return true
    }

    protected fun verifyIdentityCreateTransition(
        stateTransition: IdentityCreateTransition,
        value: ByteArray
    ): Boolean {
        val rawIdentity = stateTransition.toObject()
        rawIdentity["id"] = stateTransition.identityId.toBuffer()
        rawIdentity["revision"] = 0
        rawIdentity.remove("signature")
        rawIdentity.remove("assetLockProof")
        rawIdentity.remove("type")

        val rawIdentityFromProof = Cbor.decode(value)
        rawIdentityFromProof.remove("balance")

        val publicKeysFromProof = rawIdentityFromProof["publicKeys"] as List<Map<String, Any?>>
        val publicKeys = rawIdentity["publicKeys"] as List<Map<String, Any?>>

        if (publicKeys.size != publicKeysFromProof.size) {
            return false
        }

        for (i in publicKeys.indices) {
            if (!compareMaps(publicKeysFromProof[i], publicKeys[i])) {
                return false
            }
        }
        rawIdentity.remove("publicKeys")
        rawIdentityFromProof.remove("publicKeys")
        return compareMaps(rawIdentityFromProof, rawIdentity)
    }
}