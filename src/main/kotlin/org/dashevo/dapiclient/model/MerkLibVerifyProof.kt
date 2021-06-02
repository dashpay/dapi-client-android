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

class MerkLibVerifyProof(stateTransition: StateTransitionIdentitySigned) : DefaultVerifyProof(stateTransition) {
    override fun verify(proof: Proof): Boolean {
        val values = MerkVerifyProof.decode(proof.storeTreeProof)
        val hash = ByteArray(20) // default value
        if (values.isNotEmpty()) {
            when (stateTransition) {
                is DocumentsBatchTransition -> {
                    for (transition in stateTransition.transitions) {
                        val result = MerkVerifyProof.verifyProof(proof.storeTreeProof, transition.id.toBuffer(), hash)
                        if (result.isNotEmpty()) {
                            val match = Cbor.decode(result) == transition.toObject()
                            if (!match) {
                                return false
                            }
                        }
                    }
                    return true
                }
                is DataContractCreateTransition -> {
                    val result = MerkVerifyProof.verifyProof(proof.storeTreeProof, stateTransition.dataContract.id.toBuffer(), hash)
                    return if (result.isNotEmpty()) {
                        verifyDataContactCreateTransition(result, stateTransition)
                    } else {
                        false
                    }
                }
                is IdentityCreateTransition -> {
                    val result = MerkVerifyProof.verifyProof(proof.storeTreeProof, stateTransition.identityId.toBuffer(), hash)
                    return if (result.isNotEmpty()) {
                        verifyIdentityCreateTransition(stateTransition, result)
                    } else {
                        false
                    }
                }
            }
        }
        return false
    }
}