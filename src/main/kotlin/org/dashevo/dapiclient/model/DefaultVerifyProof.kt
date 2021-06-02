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

class DefaultVerifyProof(private val stateTransition: StateTransitionIdentitySigned) : VerifyProof() {
    override fun verify(proof: Proof): Boolean {
        val values = MerkVerifyProof.decode(proof.storeTreeProof)
        val hash = MerkVerifyProof.decodeRootHash(proof.storeTreeProof)
        if (values.isNotEmpty()) {
            when (stateTransition) {
                is DocumentsBatchTransition -> {
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
                            val match = rawDocumentFromProof.keys == rawDocument.keys
                            if (!match) {
                                return false
                            } else {
                                for (key in rawDocumentFromProof.keys) {
                                    val equal = when (rawDocumentFromProof[key]) {
                                        is ByteArray -> {
                                            when (rawDocument[key]) {
                                                is ByteArray -> (rawDocumentFromProof[key] as ByteArray).contentEquals(
                                                    rawDocument[key] as ByteArray
                                                )
                                                is Identifier -> (rawDocumentFromProof[key] as ByteArray).contentEquals(
                                                    (rawDocument[key] as Identifier).toBuffer()
                                                )
                                                else -> {
                                                    false
                                                }
                                            }
                                        }
                                        else -> {
                                            rawDocumentFromProof[key] == rawDocument[key]
                                        }
                                    }
                                    if (!equal) {
                                        println("$key doesn't match in $rawDocumentFromProof and $rawDocument")
                                        return false
                                    }
                                }
                            }
                        } else {
                            return false
                        }
                    }
                    return true
                }
                is DataContractCreateTransition -> {
                    val match = Cbor.decode(values[values.keys.first()]!!) == stateTransition.dataContract.toObject()
                    if (!match) {
                        return false
                    }
                    return true
                }
                is IdentityCreateTransition -> {
                    val rawIdentity = stateTransition.toObject()
                    rawIdentity["id"] = stateTransition.identityId.toBuffer()
                    rawIdentity["revision"] = 0
                    rawIdentity.remove("signature")
                    rawIdentity.remove("assetLockProof")
                    rawIdentity.remove("type")

                    val rawIdentityFromProof = Cbor.decode(values[values.keys.first()]!!)
                    rawIdentityFromProof.remove("balance")
                    val match = rawIdentityFromProof == rawIdentity
                    if (!match) {
                        return false
                    }
                    return true
                }
            }
        }
        return false
    }
}