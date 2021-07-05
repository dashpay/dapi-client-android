package org.dashj.platform.dapiclient.model

data class JsonRPCRequest<T: Any>(
        val method: String,
        val params: T? = null,
        val jsonrpc: String = "2.0",
        val id: String = "1"
)