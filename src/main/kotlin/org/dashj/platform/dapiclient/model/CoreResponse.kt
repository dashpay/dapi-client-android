/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.model

import org.bitcoinj.core.Sha256Hash
import org.dashj.platform.dapiclient.provider.DAPIAddress
import org.dashj.platform.dapiclient.DapiClient

/**
 * GetStatusResponse contains the response of the [DapiClient.getStatus] DAPI call with some additional
 * information that includes the timeStamp when the call was made, the address of the masternode and the
 * duration of the response time.
 */

data class Version (val protocolVersion: Int, val software: Int, val userAgent: String)
data class Time (val now: Int, val offset: Int, val median: Int)
enum class Status (val value: Int) {
    NOT_STARTED(0),
    SYNCING(1),
    READY(2),
    ERROR(3);

    companion object {
        private val values = values()
        fun getByCode(code: Int): Status {
            return values.filter { it.value == code }[0]
        }
    }
}
data class Chain(val name: String, val headerCount: Int, val blockCount: Int,
                 val bestBlockHash: Sha256Hash, val difficulty: Double,
                 val chainWork: ByteArray, val isSynced: Boolean, val syncProgress: Double)

data class Masternode(val status: Status,
                      val proTxHash: Sha256Hash,
                      val posePenalty: Int,
                      val isSynced: Boolean,
                      val syncProgress: Double) {
    enum class Status (val value: Int) {
        UNKNOWN(0),
        WAITING_FOR_PROTX(1),
        POSE_BANNED(2),
        REMOVED(3),
        OPERATOR_KEY_CHANGED(4),
        PROTX_IP_CHANGED(5),
        READY(6),
        ERROR(7);

        companion object {
            private val values = Status.values()
            fun getByCode(code: Int): Status {
                return values.filter { it.value == code }[0]
            }
        }
    }
}

data class NetworkFee(val relay: Double, val incremental: Double)

data class Network(val peerCount: Int, val fee: NetworkFee)

data class GetStatusResponse(val version: Version,
                             val time: Time,
                             val status: Status,
                             val syncProgress: Double,
                             val chain: Chain,
                             val masternode: Masternode,
                             val network: Network,
                             val timeStamp: Long,
                             val address: DAPIAddress? = null,
                             val duration: Long)