package org.dashevo.dapiclient.model

import com.google.gson.annotations.SerializedName

open class DapSpace {
    @SerializedName("dapid")
    val dapId: String = ""
    @SerializedName("buid")
    val buId: String = ""
    val objects: List<Map<String, Any>> = listOf()
    val maxidx: Int = 0
}