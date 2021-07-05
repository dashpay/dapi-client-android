package org.dashj.platform.dapiclient.model

open class JsonRPCResponse<T: Any>(
        val result: T? = null,
        val id: String,
        val jsonrpc: String,
        val error: Map<String, Any>?
)