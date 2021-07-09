/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.model

data class WaitForStateTransitionResult(val error: StateTransitionBroadcastException?, val proof: Proof?, val responseMetaData: ResponseMetaData) {
    constructor(error: StateTransitionBroadcastException, responseMetaData: ResponseMetaData) : this(error, null, responseMetaData)
    constructor(proof: Proof, responseMetaData: ResponseMetaData) : this(null, proof, responseMetaData)

    fun isSuccess(): Boolean {
        return error == null && proof != null
    }
    fun isError(): Boolean {
        return error != null && proof == null
    }
}
