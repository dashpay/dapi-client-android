/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.provider

import io.grpc.Status
import org.bitcoinj.core.Sha256Hash
import org.dashj.platform.dapiclient.model.GetStatusResponse
import java.util.Date

class DAPIAddress(
    var host: String,
    val httpPort: Int,
    val grpcPort: Int,
    val proRegTxHash: Sha256Hash
) {

    companion object {
        const val DEFAULT_JRPC_PORT = 1443
        const val DEFAULT_GRPC_PORT = 1443
    }

    var banCount: Int = 0
    var banStartTime: Long = -1L
    var lastStatus: GetStatusResponse? = null
    val exception = hashMapOf<Status.Code, Int>()

    constructor(host: String, proRegTxHash: Sha256Hash) : this(host, DEFAULT_JRPC_PORT, DEFAULT_GRPC_PORT, proRegTxHash)

    constructor(address: DAPIAddress) : this(address.host, address.httpPort, address.grpcPort, address.proRegTxHash)

    constructor(address: String) : this(address, DEFAULT_JRPC_PORT, DEFAULT_GRPC_PORT, Sha256Hash.ZERO_HASH)

    fun markAsBanned() {
        banCount++
        banStartTime = Date().time
    }

    fun markAsLive() {
        banCount = 0
        banStartTime = -1
    }

    val isBanned
        get() = banCount > 0

    override fun toString(): String {
        val sb = StringBuilder("DAPIAddress($host:$httpPort/$grpcPort")
        if (proRegTxHash != Sha256Hash.ZERO_HASH) {
            sb.append("proRegTxHash: $proRegTxHash")
        }
        sb.append(")")
        return sb.toString()
    }

    fun addException(code: Status.Code) {
        if (exception.containsKey(code)) {
            exception[code] = exception[code]!!.plus(1)
        } else {
            exception[code] = 1
        }
    }
}
