/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.model

class WaitForStateTransitionResult(val error: StateTransitionBroadcastException?, proof: Proof?, responseMetadata: ResponseMetadata) : AbstractResponse(proof, responseMetadata) {
    constructor(error: StateTransitionBroadcastException, responseMetadata: ResponseMetadata) : this(error, null, responseMetadata)
    constructor(proof: Proof, responseMetadata: ResponseMetadata) : this(null, proof, responseMetadata)

    fun isSuccess(): Boolean {
        return error == null && proof != null
    }
    fun isError(): Boolean {
        return error != null && proof == null
    }
}
