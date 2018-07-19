/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient.model

data class SubTx(
        val pver: Int,
        val uname: String,
        val pubkey: String,
        val action: Int = 1
)