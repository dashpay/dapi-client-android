package org.dashevo.dapiclient.callback

import org.dashevo.dapiclient.model.DapContext

interface GetDapContextCallback : BaseCallback {
    fun onSuccess(dapContext: DapContext)
}