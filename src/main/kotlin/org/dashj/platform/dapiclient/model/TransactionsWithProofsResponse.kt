package org.dashj.platform.dapiclient.model

import org.dash.platform.dapi.v0.CoreOuterClass

data class TransactionsWithProofsResponse(
    val rawTransactions: List<ByteArray>,
    val instantSendLocks: List<ByteArray>,
    val rawMerkleBlock: ByteArray
) {
    constructor(response: CoreOuterClass.TransactionsWithProofsResponse) :
        this(
            if (response.hasRawTransactions()) {
                response.rawTransactions.transactionsList.map { it.toByteArray() }
            } else {
                listOf<ByteArray>()
            },
            if (response.hasInstantSendLockMessages()) {
                response.instantSendLockMessages.messagesList.map { it.toByteArray() }
            } else {
                listOf<ByteArray>()
            },
            response.rawMerkleBlock.toByteArray()
        )
}
