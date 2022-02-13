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

    companion object {
        const val DRIVE_DATA_KEY = "drive-error-data-bin"
        const val ARGUMENTS_KEY = "arguments"
    }

    constructor(error: org.dash.platform.dapi.v0.PlatformOuterClass.StateTransitionBroadcastError) :
        this(error.code, error.message, error.data.toByteArray())

    private val metaData: Map<String, Any?> = try {
        Cbor.decode(data)
    } catch (e: Exception) {
        mapOf()
    }
    private val driveErrorData: Map<String, Any?> = metaData[DRIVE_DATA_KEY]?.let {
        Cbor.decode(it as ByteArray)
    } ?: mapOf()
    val exception: ConcensusException = ConcensusException.create(code, driveErrorData[ARGUMENTS_KEY] as List<Any>)

    override fun toString(): String {
        return errorMessage
    }
}
