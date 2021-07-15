package org.dashj.platform.dapiclient.grpc

import org.dashj.platform.dapiclient.model.TransactionsWithProofsResponse

interface SubscribeToTransactionsWithProofs {
    fun onNext(value: TransactionsWithProofsResponse?)
    fun onError(t: Throwable?)
    fun onCompleted()
}
