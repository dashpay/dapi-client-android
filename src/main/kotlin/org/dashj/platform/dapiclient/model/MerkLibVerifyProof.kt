package org.dashj.platform.dapiclient.model

import org.dashj.merk.ByteArrayKey
import org.dashj.merk.MerkVerifyProof
import org.dashj.platform.dpp.contract.DataContractCreateTransition
import org.dashj.platform.dpp.document.DocumentsBatchTransition
import org.dashj.platform.dpp.identity.IdentityCreateTransition
import org.dashj.platform.dpp.statetransition.StateTransitionIdentitySigned

class MerkLibVerifyProof(stateTransition: StateTransitionIdentitySigned/*, val expectedHash: ByteArray*/) : DefaultVerifyProof(stateTransition) {
    override fun verify(proof: Proof): Boolean {
        val values = MerkVerifyProof.decode(proof.storeTreeProof)
        if (values.isNotEmpty()) {
            when (stateTransition) {
                is DocumentsBatchTransition -> {
                    val verifyMap = hashMapOf<ByteArrayKey, ByteArray>()
                    for (transition in stateTransition.transitions) {
                        val result = MerkVerifyProof.extractProof(proof.storeTreeProof)
                        if (result.isNotEmpty()) {
                            val key = ByteArrayKey(transition.id.toBuffer())
                            verifyMap[key] = result[key]!!
                        } else {
                            return false
                        }
                    }
                    return verifyDocumentsBatchTransition(stateTransition, verifyMap)
                }
                is DataContractCreateTransition -> {
                    val result = MerkVerifyProof.extractProof(proof.storeTreeProof)

                    return if (result.isNotEmpty()) {
                        verifyDataContactCreateTransition(result[ByteArrayKey(stateTransition.dataContract.id.toBuffer())]!!, stateTransition)
                    } else {
                        false
                    }
                }
                is IdentityCreateTransition -> {
                    val result = MerkVerifyProof.extractProof(proof.storeTreeProof)
                    return if (result.isNotEmpty()) {
                        verifyIdentityCreateTransition(stateTransition, result[ByteArrayKey(stateTransition.identityId.toBuffer())]!!)
                    } else {
                        false
                    }
                }
            }
        }
        return false
    }
}
