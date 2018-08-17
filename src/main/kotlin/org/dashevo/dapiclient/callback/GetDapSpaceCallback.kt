package org.dashevo.dapiclient.callback

interface GetDapSpaceCallback<T> : BaseCallback {
    fun onSuccess(dapSpace: T)
}