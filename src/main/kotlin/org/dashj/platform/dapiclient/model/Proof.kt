/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.model

import org.dashj.platform.dpp.toHexString

data class Proof(val rootTreeProof: ByteArray, val storeTreeProof: StoreTreeProofs, val signatureLlmqHash: ByteArray, val signature: ByteArray) {
    constructor(proof: org.dash.platform.dapi.v0.PlatformOuterClass.Proof) : this(proof.rootTreeProof.toByteArray(), StoreTreeProofs(proof.storeTreeProofs), proof.signatureLlmqHash.toByteArray(), proof.signature.toByteArray())

    val type = when {
        storeTreeProof.documentsProof.isNotEmpty() -> "documentProof"
        storeTreeProof.identitiesProof.isNotEmpty() -> "identitiesProof"
        storeTreeProof.dataContractsProof.isNotEmpty() -> "dataContractsProof"
        storeTreeProof.publicKeyHashesToIdentityIdsProof.isNotEmpty() -> "publicKeyHashesToIdentityIdsProof"
        else -> "none"
    }

    fun isValid(): Boolean {
        return rootTreeProof.isNotEmpty() && signature.isNotEmpty()
    }

    override fun toString(): String {
        return "Proof(rootTreeProof: ${rootTreeProof.toHexString()}\n  storeTreeProof: $storeTreeProof\n  signatureLlmqHash: ${signatureLlmqHash.toHexString()}\n  signature: ${signature.toHexString()}"
    }
}
