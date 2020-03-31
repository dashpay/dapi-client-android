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
import okhttp3.logging.HttpLoggingInterceptor
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.dashevo.dapiclient.callback.DapiRequestCallback
import org.dashevo.dapiclient.extensions.enqueue
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.FetchDapObjectParams
import org.dashevo.dapiclient.model.JsonRPCRequest
import org.dashevo.dapiclient.model.JsonRPCResponse
import org.dashevo.dapiclient.rest.DapiService
import org.dashevo.dpp.contract.Contract
import org.dashevo.dpp.contract.ContractFactory
import org.dashevo.dpp.contract.ContractStateTransition
import org.dashevo.dpp.document.Document
import org.dashevo.dpp.document.DocumentsStateTransition
import org.dashevo.dpp.statetransition.StateTransition
import org.dashevo.dpp.toHexString
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

open class OldDapiClient(mnIP: String, mnDapiPort: String, debug: Boolean = false) {

    companion object {
        private val logger = Logger.getLogger(OldDapiClient::class.java.name)
        val DEFAULT_JRPC_PORT = 3000
    }

    private val retrofit: Retrofit

    val dapiService: DapiService

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val debugOkHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .build()
        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://$mnIP:$mnDapiPort/")
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
    open fun registerDap(dapSchema: Map<String, Any>, userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash,
                         privKey: ECKey, cb: DapiRequestCallback<String>) {

        //sendStateTransition(ContractStateTransition(Contract("", mutableMapOf())), userRegTxId, hashPrevSubTx, privKey, cb)
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
    fun sendDapObject(dapObject: MutableMap<String, Any?>, dapId: String, userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash,
                      privKey: ECKey, cb: DapiRequestCallback<String>) {

        val document = Document(dapObject)
        val stPacket = DocumentsStateTransition(listOf(document))
        //sendStateTransition(stPacket, userRegTxId, hashPrevSubTx, privKey, cb)
    }

    /**
     * Send State Transition.
     * @param stPacket: data packet.
     * @param userRegTxId: Transaction ID of User creating the object.
     */
    /*fun sendStateTransition(stPacket: StateTransition, userRegTxId: Sha256Hash, hashPrevSubTx: Sha256Hash, privKey: ECKey,
                            cb: DapiRequestCallback<String>) {

        val serializedPacket = stPacket.serialize()
        val stPacketHash = Sha256Hash.wrap(stPacket.hash())

        //val stateTransitionTx = SubTxTransition(1, userRegTxId, hashPrevSubTx, Coin.valueOf(1000),
        //        stPacketHash, privKey)

        val serializedStHex = stPacket.serialize(false).toHexString()
        val serializedPacketHex = serializedPacket.toHexString()

        sendRawTransition(serializedStHex, serializedPacketHex, object : DapiRequestCallback<String> {
            override fun onSuccess(data: JsonRPCResponse<String>) {
                cb.onSuccess(data)
            }

            override fun onError(errorMessage: String) {
                cb.onError(errorMessage)
            }
        })
    }*/

    /**
     * Get blockchain user.
     * @param username: Username of registered user.
     * @param cb: Callback.
     */
    /*fun getUser(username: String, cb: DapiRequestCallback<BlockchainUser>) {
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
    }*/

    /**
     * Fetch DAP Contract.
     * @param contractId: DAP ID of registered contract.
     * @param cb: Callback.
     */
    /*fun fetchContract(contractId: String, cb: DapiRequestCallback<Contract>) {
        val request = JsonRPCRequest("fetchContract",
                mapOf("contractId" to contractId))
        dapiService.fetchContract(request).enqueue({
            val body = it.body()
            if (it.isSuccessful && body != null) {
                if (body.error != null) {
                    cb.onError(body.error["message"] as String)
                } else {
                    val contract = ContractFactory().create(contractId, body.result!!.toMutableMap())
                    cb.onSuccess(JsonRPCResponse(contract, body.id, body.jsonrpc, body.error))
                }
            } else {
                cb.onError(it.message())
            }
        }, {
            cb.onError(it.localizedMessage)
        })
    }*/

    /**
     * Fetch DAP Objects
     * @param dapId: ID of the DAP under which objects were registered.
     * @param type: type of object ot be fetched, this type is defined under a DAP's Contract.
     * @param cb: Callback.
     * @param query: query to filter objects.
     */
    /*inline fun <reified T : Any> fetchObjects(dapId: String, type: String, cb: DapiRequestCallback<List<T>>,
                                              query: Map<String, Any> = mapOf()) {

        val request = JsonRPCRequest("fetchObjects",
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
    }*/

    /**
     * Send a Raw State Transition.
     * @param header State Transition Header in hex format.
     * @param packet State Transition Packet in hex format.
     * @param cb interface to be called after Dapi call is finished.
     */
    /*fun sendRawTransition(header: String, packet: String, cb: DapiRequestCallback<String>) {
        dapiService.sendRawTransition(JsonRPCRequest("sendRawTransition", mapOf("rawStateTransition" to header,
                "rawSTPacket" to packet))).enqueue({
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
    }*/

    fun getBestBlockHash(cb: DapiRequestCallback<String>) {
        dapiService.getBestBlockHash(JsonRPCRequest("getBestBlockHash", mapOf())).enqueue({
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

    fun getBestBlockHash() : String? {
        val response = dapiService.getBestBlockHash(JsonRPCRequest("getBestBlockHash", mapOf())).execute()
        if(response.isSuccessful) {
            return response.body()!!.result
        } else {
            throw Exception("jRPC error code: ${response.code()})")
        }
    }
}