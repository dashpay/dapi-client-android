/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient.callback

import org.dashevo.dapiclient.model.BlockchainUserContainer

interface GetUserCallback : BaseCallback {
    fun onSuccess(blockchainUserContainer: BlockchainUserContainer)
}