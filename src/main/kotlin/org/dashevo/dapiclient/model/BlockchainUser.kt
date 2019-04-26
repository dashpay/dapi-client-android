/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient.model

data class BlockchainUser(
        val uname: String,
        val regtxid: String,
        val pubkeyid: String,
        val credits: Long,
        val data: String,
        //TODO: convert to enum
        val state: String,
        val subtx: ArrayList<String>
)