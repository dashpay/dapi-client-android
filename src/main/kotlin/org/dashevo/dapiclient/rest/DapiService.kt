/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.rest

import com.google.gson.JsonElement
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.FetchDapObjectParams
import org.dashevo.dapiclient.model.JsonRPCRequest
import org.dashevo.dapiclient.model.JsonRPCResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface DapiService {

    @POST("/")
    fun getUser(@Body body: JsonRPCRequest<Map<String, String>>) : Call<JsonRPCResponse<BlockchainUser>>

    @POST("/")
    fun fetchDapContract(@Body body: JsonRPCRequest<Map<String, String>>) : Call<JsonRPCResponse<HashMap<String, String>>>

    @POST("/")
    fun fetchDapObjects (@Body body: JsonRPCRequest<FetchDapObjectParams>) : Call<JsonRPCResponse<List<JsonElement>>>

    @POST("/")
    fun sendRawTransition(@Body body: JsonRPCRequest<Map<String, String>>) : Call<JsonRPCResponse<String>>

}