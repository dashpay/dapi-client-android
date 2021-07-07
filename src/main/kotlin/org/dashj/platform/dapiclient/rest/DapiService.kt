/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.rest
import org.dashj.platform.dapiclient.model.JsonRPCRequest
import org.dashj.platform.dapiclient.model.JsonRPCResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface DapiService {

    /**
     * Get the best block hash (block hash of the tip of the blockchain)
     * @param body JsonRPCRequest<Map<String, String>> This should be an empty map
     * @return Call<JsonRPCResponse<String>>
     */
    @POST("/")
    fun getBestBlockHash(@Body body: JsonRPCRequest<Map<String, String>>): Call<JsonRPCResponse<String>>

    /**
     * Get the block hash for the specified height
     * @param body JsonRPCRequest<Map<String, Int>>
     *      "height" = height for blockhash
     * @return Call<JsonRPCResponse<String>>
     */
    @POST("/")
    fun getBlockHash(@Body body: JsonRPCRequest<Map<String, Int>>): Call<JsonRPCResponse<String>>

    /**
     * Get the masternode list difference between two block hashes
     * @param body JsonRPCRequest<Map<String, String>>
     *     "baseBlockHash" = hash
     *     "blockHash" = hash
     * @return Call<JsonRPCResponse<Map<String, Any>>>
     */
    @POST("/")
    fun getMnListDiff(@Body body: JsonRPCRequest<Map<String, String>>): Call<JsonRPCResponse<Map<String, Any>>>
}
