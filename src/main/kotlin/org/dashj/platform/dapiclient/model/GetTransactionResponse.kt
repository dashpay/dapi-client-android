package org.dashj.platform.dapiclient.model

import org.bitcoinj.core.Sha256Hash

data class GetTransactionResponse(
    val transaction: ByteArray,
    val blockHash: Sha256Hash,
    val height: Int,
    val confirmation: Int,
    val isInstantLocked: Boolean,
    val isChainLocked: Boolean
)
