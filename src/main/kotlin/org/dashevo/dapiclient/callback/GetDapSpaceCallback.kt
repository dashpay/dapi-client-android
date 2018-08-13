package org.dashevo.dapiclient.callback

import org.dashevo.dapiclient.model.DapSpace

interface GetDapSpaceCallback : BaseCallback {
    fun onSuccess(dapSpace: DapSpace)
}