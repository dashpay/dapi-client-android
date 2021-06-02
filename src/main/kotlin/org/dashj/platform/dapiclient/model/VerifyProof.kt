package org.dashj.platform.dapiclient.model

abstract class VerifyProof() {
    abstract fun verify(proof: Proof): Boolean
}
