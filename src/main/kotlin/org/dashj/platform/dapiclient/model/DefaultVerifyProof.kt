package org.dashj.platform.dapiclient.model

import org.dashj.merk.ByteArrayKey
import org.dashj.merk.MerkVerifyProof
import org.dashj.platform.dpp.contract.DataContractCreateTransition
import org.dashj.platform.dpp.document.DocumentCreateTransition
import org.dashj.platform.dpp.document.DocumentDeleteTransition
import org.dashj.platform.dpp.document.DocumentReplaceTransition
import org.dashj.platform.dpp.document.DocumentsBatchTransition
import org.dashj.platform.dpp.identifier.Identifier
import org.dashj.platform.dpp.identity.IdentityCreateTransition
import org.dashj.platform.dpp.statetransition.StateTransitionIdentitySigned
import org.dashj.platform.dpp.util.Cbor

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
                                    // TODO: determine what goes here
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
