package org.dashevo.dapiclient.model

import org.dashevo.dpp.contract.DataContractCreateTransition
import org.dashevo.dpp.document.DocumentsBatchTransition
import org.dashevo.dpp.identity.IdentityCreateTransition
import org.dashevo.dpp.statetransition.StateTransitionIdentitySigned
import org.dashj.merk.ByteArrayKey
import org.dashj.merk.MerkVerifyProof

class MerkLibVerifyProof(stateTransition: StateTransitionIdentitySigned, val expectedHash: ByteArray) : DefaultVerifyProof(stateTransition) {
    override fun verify(proof: Proof): Boolean {
        val values = MerkVerifyProof.decode(proof.storeTreeProof)
        if (values.isNotEmpty()) {
            when (stateTransition) {
                is DocumentsBatchTransition -> {
                    val verifyMap = hashMapOf<ByteArrayKey, ByteArray>()
                    for (transition in stateTransition.transitions) {
                        val result = MerkVerifyProof.verifyProof(proof.storeTreeProof, transition.id.toBuffer(), expectedHash)
                        if (result.isNotEmpty()) {
                            verifyMap[ByteArrayKey(transition.id.toBuffer())] = result
                        } else {
                            return false
                        }
                    }
                    return verifyDocumentsBatchTransition(stateTransition, verifyMap)
                }
                is DataContractCreateTransition -> {
                    val result = MerkVerifyProof.verifyProof(proof.storeTreeProof, stateTransition.dataContract.id.toBuffer(), expectedHash)
                    return if (result.isNotEmpty()) {
                        verifyDataContactCreateTransition(result, stateTransition)
                    } else {
                        false
                    }
                }
                is IdentityCreateTransition -> {
                    val result = MerkVerifyProof.verifyProof(proof.storeTreeProof, stateTransition.identityId.toBuffer(), expectedHash)
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