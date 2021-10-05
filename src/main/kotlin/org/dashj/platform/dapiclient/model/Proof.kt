/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.model

import org.dash.platform.dapi.v0.PlatformOuterClass
import org.dashj.platform.dpp.toHex

data class Proof(
    val rootTreeProof: ByteArray,
    val storeTreeProofs: StoreTreeProofs,
    val signatureLlmqHash: ByteArray,
    val signature: ByteArray
) {
    constructor(proof: org.dash.platform.dapi.v0.PlatformOuterClass.Proof) :
    this(
        proof.rootTreeProof.toByteArray(),
        StoreTreeProofs(proof.storeTreeProofs),
        proof.signatureLlmqHash.toByteArray(),
        proof.signature.toByteArray()
    )
    constructor(proofBytes: ByteArray) : this(PlatformOuterClass.Proof.parseFrom(proofBytes))

    val type = when {
        storeTreeProofs.documentsProof.isNotEmpty() -> "documentProof"
        storeTreeProofs.identitiesProof.isNotEmpty() -> "identitiesProof"
        storeTreeProofs.dataContractsProof.isNotEmpty() -> "dataContractsProof"
        storeTreeProofs.publicKeyHashesToIdentityIdsProof.isNotEmpty() -> "publicKeyHashesToIdentityIdsProof"
        else -> "none"
    }

    fun isValid(): Boolean {
        return rootTreeProof.isNotEmpty() && signature.isNotEmpty()
    }

    override fun toString(): String {
        return "Proof(rootTreeProof: ${rootTreeProof.toHex()}\n" +
            "  storeTreeProof: $storeTreeProofs\n" +
            "  signatureLlmqHash: ${signatureLlmqHash.toHex()}\n" +
            "  signature: ${signature.toHex()}"
    }
}
