/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.model

import org.dash.platform.dapi.v0.PlatformOuterClass

class GetDataContractResponse(val dataContract: ByteArray, proof: Proof, metadata: ResponseMetadata) :
    AbstractResponse(proof, metadata) {
    constructor(response: PlatformOuterClass.GetDataContractResponse) :
        this(response.dataContract.toByteArray(), Proof(response.proof), ResponseMetadata(response.metadata))

    override fun toString(): String {
        return "GetDataContractResponse(dataContract: ${dataContract.size} bytes, proof: ${proof?.type}, $metadata)"
    }
}
