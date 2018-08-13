package org.dashevo.dapiclient.callback

import org.dashevo.dapiclient.model.BlockchainUser

interface LoginCallback : BaseCallback {
    fun onSuccess(blockchainUser: BlockchainUser)
}