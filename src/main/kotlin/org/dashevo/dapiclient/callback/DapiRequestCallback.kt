package org.dashevo.dapiclient.callback

import org.dashevo.dapiclient.model.JsonRPCResponse

interface DapiRequestCallback<T : Any> : BaseCallback {
    fun onSuccess(data: JsonRPCResponse<T>)
}