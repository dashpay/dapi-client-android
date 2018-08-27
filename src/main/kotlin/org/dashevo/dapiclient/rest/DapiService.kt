/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.rest

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.dashevo.dapiclient.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DapiService {

    @POST("users")
    fun createUser(@Body subTx: JsonObject): Call<TxId>

    @GET("users/search/{query}")
    fun searchUsers(@Path("query") query: String): Call<List<BlockchainUserContainer>>

    @GET("users/{idOrUsername}")
    fun getUser(@Path("idOrUsername") idOrUsername: String): Call<BlockchainUserContainer>

    @POST("dap")
    fun postDap(@Body dapUpdate: JsonObject): Call<TxId>

    @GET("dap/{dapId}")
    fun getDap(@Path("dapId") dapId: String): Call<JsonObject>

    @GET("dap/space/{dapId}/{buId}")
    fun getDapSpace(@Path("dapId") dapId: String, @Path("buId") buId: String) : Call<DapSpace>

    @GET("dap/context/{dapId}/{buId}")
    fun getDapContext(@Path("dapId") dapId: String, @Path("buId") buId: String) : Call<DapContext>

}