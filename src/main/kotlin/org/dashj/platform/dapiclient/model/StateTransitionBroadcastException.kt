/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.model

import org.dashj.platform.dpp.errors.concensus.ConcensusException
import org.dashj.platform.dpp.util.Cbor

class StateTransitionBroadcastException(val code: Int, val errorMessage: String, val data: ByteArray) :
    Exception("$code: $errorMessage") {

    constructor(error: org.dash.platform.dapi.v0.PlatformOuterClass.StateTransitionBroadcastError) :
    this(error.code, error.message, error.data.toByteArray())

    private val metaData: Map<String, Any?>
    val driveErrorData: Map<String, Any?>
    val exception: ConcensusException

    init {
        metaData = try {
            Cbor.decode(data)
        } catch (e: Exception) {
            println("$e processing $data")
            mapOf()
        }
        driveErrorData = Cbor.decode(metaData["drive-error-data-bin"] as ByteArray)
        exception = ConcensusException.create(code, driveErrorData["arguments"] as List<Any>)
    }

    override fun toString(): String {
        return errorMessage
    }
}
