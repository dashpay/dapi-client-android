package org.dashevo.dapiclient.model

import io.grpc.StatusRuntimeException
import org.json.JSONObject

class GrpcExceptionInfo (trailers: String) {
    val errors = arrayListOf<Map<String, Any>>()

    constructor(statusRuntimeException: StatusRuntimeException) : this(statusRuntimeException.trailers.toString())

    init {
        val cursor = trailers.findAnyOf(listOf("errors="))
        val end = trailers.findLastAnyOf(listOf(")"))
        val endpos = end?.first ?: trailers.length
        if (cursor != null) {
            val errorString = trailers.substring(cursor.first+ 7, endpos)

            val errorMap = JSONObject("{errors: $errorString}").toMap()
            errors.addAll(errorMap["errors"] as List<Map<String, Any>>)
        }
    }

    override fun toString(): String {
        return errors.toString()
    }
}