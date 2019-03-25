/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import org.bitcoinj.core.Coin
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.evolution.SubTxTransition
import org.dashevo.dapiclient.callback.DapiRequestCallback
import org.dashevo.dapiclient.extensions.enqueue
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.FetchDapObjectParams
import org.dashevo.dapiclient.model.JsonRPCRequest
import org.dashevo.dapiclient.model.JsonRPCResponse
import org.dashevo.dapiclient.rest.DapiService
import org.dashevo.schema.Create
import org.dashevo.schema.Object
import org.dashevo.schema.Serialize
import org.dashevo.schema.toHexString
import org.jsonorg.JSONArray
import org.jsonorg.JSONException
import org.jsonorg.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

open class DapiClient(mnIP: String, mnDapiPort: String, debug: Boolean = false) {

    private val retrofit: Retrofit

    val dapiService: DapiService

    init {
        val debugOkHttpClient = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .build()
        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("$mnIP:$mnDapiPort/")
                .client(if (debug) debugOkHttpClient else OkHttpClient())
                .build()
        dapiService = retrofit.create(DapiService::class.java)
    }

    inline fun <reified T> Gson.fromJson(json: JsonElement): T = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

    /**
     * Create a DAP
     * @param dapSchema Schema for the DAP being Created
     * @param userRegTxId hash of Blockchain User registration transaction (SubTx)
     * @param cb callback, instance of {@link DapCallback}
     */
    open fun registerDap(dapSchema: JSONObject, userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash,
                         privKey: ECKey, cb: DapiRequestCallback<String>) {
        val dapContract: JSONObject? = try {
            Create.createDapContract(dapSchema)
        } catch (e: JSONException) {
            cb.onError(e.localizedMessage)
            return
        }
        dapContract!!.put("pver", 1)

        val stPacket = Create.createSTPacketInstance()
        stPacket.getJSONObject(Object.STPACKET).put("dapcontract", dapContract.getJSONObject("dapcontract"))

        sendStateTransition(stPacket, userRegTxId, hashPrevSubTx, privKey, cb)
    }

    /**
     * Send DAP Object.
     * @param dapObject: Object to be registered.
     * @param dapId: DAP ID that the object belongs to.
     * @param userRegTxId: Transaction ID of User creating the object.
     * @param hashPrevSubTx: Transaction ID of User's last State Transition or [userRegTxId] if it's the first one.
     * @param privKey: Private Key of User's pubKey.
     * @param cb: Callback.
     */
    fun sendDapObject(dapObject: JSONObject, dapId: String, userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash,
                      privKey: ECKey, cb: DapiRequestCallback<String>) {

        val dapObjects = JSONArray()
        dapObjects.put(dapObject)

        val stPacket = Create.createSTPacketInstance()
        stPacket.getJSONObject(Object.STPACKET).put("dapobjects", dapObjects)
        stPacket.getJSONObject(Object.STPACKET).put("dapid", dapId)

        sendStateTransition(stPacket, userRegTxId, hashPrevSubTx, privKey, cb)
    }

    /**
     * Send State Transition.
     * @param stPacket: data packet.
     * @param userRegTxId: Transaction ID of User creating the object.
     */
    fun sendStateTransition(stPacket: JSONObject, userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash, privKey: ECKey,
                            cb: DapiRequestCallback<String>) {

        val serializedPacket = Serialize.encode(stPacket.getJSONObject("stpacket"))
        val stPacketHash = Sha256Hash.wrap(Sha256Hash.hashTwice(serializedPacket))

        val stateTransitionTx = SubTxTransition(1, userRegTxId, hashPrevSubTx, Coin.valueOf(1000),
                stPacketHash, privKey)

        val serializedStHex = stateTransitionTx.unsafeBitcoinSerialize().toHexString()
        val serializedPacketHex = serializedPacket.toHexString()

        sendRawTransition(serializedStHex, serializedPacketHex, object : DapiRequestCallback<String> {
            override fun onSuccess(data: JsonRPCResponse<String>) {
                cb.onSuccess(data)
            }

            override fun onError(errorMessage: String) {
                cb.onError(errorMessage)
            }
        })
    }

    /**
     * Get blockchain user.
     * @param username: Username of registered user.
     * @param cb: Callback.
     */
    fun getUser(username: String, cb: DapiRequestCallback<BlockchainUser>) {
        dapiService.getUser(JsonRPCRequest("getUser", mapOf("username" to username))).enqueue(fun(it: Response<JsonRPCResponse<BlockchainUser>>) {
            val body = it.body()
            if (it.isSuccessful && body != null) {
                if (body.error != null) {
                    cb.onError(body.error["message"] as String)
                } else {
                    cb.onSuccess(body)
                }
            } else {
                cb.onError(it.message())
            }
        }, {
            cb.onError(it.localizedMessage)
        })
    }

    /**
     * Fetch DAP Contract.
     * @param dapId: DAP ID of registered contract.
     * @param cb: Callback.
     */
    fun fetchDapContract(dapId: String, cb: DapiRequestCallback<HashMap<String, Any>>) {
        val request = JsonRPCRequest("fetchDapContract", mapOf("dapId" to dapId))
        dapiService.fetchDapContract(request).enqueue({
            val body = it.body()
            if (it.isSuccessful && body != null) {
                if (body.error != null) {
                    cb.onError(body.error["message"] as String)
                } else {
                    cb.onSuccess(body)
                }
            } else {
                cb.onError(it.message())
            }
        }, {
            cb.onError(it.localizedMessage)
        })
    }

    /**
     * Fetch DAP Objects
     * @param dapId: ID of the DAP under which objects were registered.
     * @param type: type of object ot be fetched, this type is defined under a DAP's Contract.
     * @param cb: Callback.
     * @param query: query to filter objects.
     */
    inline fun <reified T : Any> fetchDapObjects(dapId: String, type: String, cb: DapiRequestCallback<List<T>>,
                                                 query: Map<String, Any> = mapOf()) {

        val request = JsonRPCRequest("fetchDapObjects",
                FetchDapObjectParams(dapId, type, mapOf("where" to query)))
        dapiService.fetchDapObjects(request).enqueue({ it ->
            val gson = Gson()
            val list = ArrayList<T>()

            val body = it.body()
            if (it.isSuccessful && body != null) {
                if (body.error != null) {
                    cb.onError(body.error["message"] as String)
                } else {
                    body.result?.forEach {
                        list.add(gson.fromJson(it))
                    }
                    cb.onSuccess(JsonRPCResponse(list, body.id, "", null))
                }
            } else {
                cb.onError(it.message())
            }
        }, {
            cb.onError(it.localizedMessage)
        })
    }

    /**
     * Send a Raw State Transition.
     * @param header State Transition Header in hex format.
     * @param packet State Transition Packet in hex format.
     * @param cb interface to be called after Dapi call is finished.
     */
    fun sendRawTransition(header: String, packet: String, cb: DapiRequestCallback<String>) {
        dapiService.sendRawTransition(JsonRPCRequest("sendRawTransition", mapOf("rawTransitionHeader" to header,
                "rawTransitionPacket" to packet))).enqueue({
            val body = it.body()
            if (it.isSuccessful && body != null) {
                if (body.error != null) {
                    cb.onError(body.error["message"] as String)
                } else {
                    cb.onSuccess(body)
                }
            } else {
                cb.onError(it.message())
            }
        }, {
            cb.onError(it.localizedMessage)
        })
    }

}