/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.model

import org.dashj.platform.dpp.toHex

class StoreTreeProofs(
    val identitiesProof: ByteArray,
    val publicKeyHashesToIdentityIdsProof: ByteArray,
    val dataContractsProof: ByteArray,
    val documentsProof: ByteArray
) {
    constructor(storeTreeProofs: org.dash.platform.dapi.v0.PlatformOuterClass.StoreTreeProofs) :
    this(
        storeTreeProofs.identitiesProof.toByteArray(),
        storeTreeProofs.publicKeyHashesToIdentityIdsProof.toByteArray(),
        storeTreeProofs.dataContractsProof.toByteArray(),
        storeTreeProofs.documentsProof.toByteArray()
    )

    override fun toString(): String {
        return "StoreTreeProofs(\n" +
            " identitiesProof: ${identitiesProof.toHex()}\n" +
            " publicKeyHashesToIdentityIdsProof: ${publicKeyHashesToIdentityIdsProof.toHex()}\n" +
            " dataContractsProof: ${dataContractsProof.toHex()}\n" +
            " documentsProof: ${documentsProof.toHex()})"
    }
}
