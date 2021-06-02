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

    private fun verifyDocumentsBatchTransition(
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
        return rawIdentityFromProof == rawIdentity
    }
}
