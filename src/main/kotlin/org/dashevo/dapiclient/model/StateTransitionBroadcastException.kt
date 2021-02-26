/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashevo.dapiclient.model

import org.dashevo.dpp.util.Cbor

class StateTransitionBroadcastException(val code: Int, val errorMessage: String, val data: ByteArray)
    : Exception("$code: $errorMessage") {

    constructor(error: org.dash.platform.dapi.v0.PlatformOuterClass.StateTransitionBroadcastError) :
            this(error.code, error.message, error.data.toByteArray())

    var dataMap: Map<String, Any?>

    init {
        try {
            dataMap = Cbor.decode(data)
        } catch (e: Exception) {
            println("$e processing $data")
            dataMap = mapOf()
        }
    }

    val errors: List<Any> by lazy {
        if (dataMap.containsKey("errors")) (dataMap["errors"] as List<Any>) else listOf()
    }

    override fun toString(): String {
        return "$errorMessage: $errors"
    }
}