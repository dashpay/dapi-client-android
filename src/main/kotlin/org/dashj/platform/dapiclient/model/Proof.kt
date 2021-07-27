/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.model

import org.dashj.platform.dpp.toHexString

data class Proof(val rootTreeProof: ByteArray, val storeTreeProof: ByteArray, val signatureLlmqHash: ByteArray, val signature: ByteArray) {
    constructor(proof: org.dash.platform.dapi.v0.PlatformOuterClass.Proof) : this(proof.rootTreeProof.toByteArray(), proof.storeTreeProof.toByteArray(), proof.signatureLlmqHash.toByteArray(), proof.signature.toByteArray())

    fun isValid(): Boolean {
        return rootTreeProof.isNotEmpty() && signature.isNotEmpty()
    }

    override fun toString(): String {
        return "Proof(rootTreeProof: ${rootTreeProof.toHexString()}\n  storeTreeProof: ${storeTreeProof.toHexString()}\n  signatureLlmqHash: ${signatureLlmqHash.toHexString()}\n  signature: ${signature.toHexString()}"
    }
}
