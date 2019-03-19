package org.dashevo.dapiclient.model

data class FetchDapObjectParams(val dapId: String, val type: String,
                                val options: Map<String, Map<String, Any>>)