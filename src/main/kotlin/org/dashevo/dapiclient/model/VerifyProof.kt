package org.dashevo.dapiclient.model

abstract class VerifyProof() {
    abstract fun verify(proof: Proof): Boolean
}