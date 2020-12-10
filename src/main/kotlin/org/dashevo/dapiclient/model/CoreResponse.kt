/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.model

import org.dashevo.dapiclient.provider.DAPIAddress
import org.dashevo.dapiclient.DapiClient

/**
 * GetStatusResponse contains the response of the [DapiClient.getStatus] DAPI call with some additional
 * information that includes the timeStamp when the call was made, the address of the masternode and the
 * duration of the response time.
 */
data class GetStatusResponse(val coreVersion: Int,
                             val protocolVersion: Int,
                             val blocks: Int,
                             val timeOffset: Int,
                             val connections: Int,
                             val proxy: String,
                             val difficulty: Double,
                             val testnet: Boolean,
                             val relayFee: Double,
                             val errors: String,
                             val network: String,
                             val timeStamp: Long,
                             val address: DAPIAddress? = null,
                             val duration: Long)