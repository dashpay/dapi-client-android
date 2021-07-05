/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.model

data class WaitForStateTransitionResult(val error: StateTransitionBroadcastException?, val proof: Proof?) {
    constructor(error: StateTransitionBroadcastException) : this(error, null)
    constructor(proof: Proof) : this(null, proof)

    fun isSuccess(): Boolean {
        return error == null && proof != null
    }
    fun isError(): Boolean {
        return error != null && proof == null
    }
}
