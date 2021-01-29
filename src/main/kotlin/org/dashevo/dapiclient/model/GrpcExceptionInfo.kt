package org.dashevo.dapiclient.model

import io.grpc.StatusRuntimeException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types


class GrpcExceptionInfo (trailers: String) {
    val errors = arrayListOf<Map<String, Any>>()

    constructor(statusRuntimeException: StatusRuntimeException) : this(statusRuntimeException.trailers.toString())

    init {
        val cursor = trailers.findAnyOf(listOf("errors="))
        val end = trailers.findLastAnyOf(listOf(")"))
        val endpos = end?.first ?: trailers.length
        if (cursor != null) {
            val errorString = trailers.substring(cursor.first+ 7, endpos)
            val json = "{errors: $errorString }"
            val moshi = Moshi.Builder().build()
            val map = Types.newParameterizedType(MutableMap::class.java, String::class.java, Any::class.java)
            val adapter: JsonAdapter<Map<String, Any>> = moshi.adapter<Map<String, Any>>(map).lenient()
            val errorMap = adapter.fromJson(json)
            if (errorMap != null && errorMap.containsKey("errors")) {
                errors.addAll(errorMap["errors"] as List<Map<String, Any>>)
            }
        }
    }

    override fun toString(): String {
        return errors.toString()
    }
}