/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.dashevo.dapiclient.callback.*
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.BlockchainUserContainer
import org.dashevo.dapiclient.model.SubTx
import org.dashevo.dapiclient.rest.DapiService
import org.dashevo.schema.Create
import org.dashevo.schema.Object
import org.dashevo.schema.Validate
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class DapiClient(masterNodeIP: String) {

    private val retrofit: Retrofit
    private val dapiService: DapiService
    private val gson = Gson()
    private var dapContract = JSONObject()
    private var dapContext = JSONObject()
    private var currentUser: BlockchainUser? = null

    companion object {
        private const val MN_DAPI_PORT = "8080"
        private const val PVER = 1
        private const val CREATE_OBJECT_ACTION = 1
        private const val UPDATE_OBJECT_ACTION = 2
        private const val REMOVE_OBJECT_ACTION = 3
    }

    init {
        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://$masterNodeIP:$MN_DAPI_PORT/")
                .build()
        dapiService = retrofit.create(DapiService::class.java)
        //TODO: For testing only
        dapContract = JSONObject(File(DapiClient::class.java.getResource("/dashpay-dapcontract.json").path).readText())
    }

    /**
     * Extension function to modify retrofit callback to use lambdas
     */
    private fun <T> Call<T>.enqueue(success: (response: Response<T>) -> Unit,
                                    failure: (t: Throwable) -> Unit) {
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>?, response: Response<T>) = success(response)

            override fun onFailure(call: Call<T>?, t: Throwable) = failure(t)
        })
    }

    /**
     * Create a JSONObject to comply with Schema SysObject format
     */
    private fun createJSONContainer(key: String, obj: Any): JSONObject {
        return JSONObject(hashMapOf(key to obj))
    }

    private fun getDapContractId(): String? {
        return dapContract.getJSONObject("dapcontract").getJSONObject("meta").getString("dapid")
    }

    private fun getDapContractSchema(): JSONObject {
        return dapContract.getJSONObject("dapcontract").getJSONObject("dapschema")
    }

    private fun checkAuth(cb: BaseCallback): Boolean {
        if (currentUser?.uid == null) {
            cb.onError("User session null or invalid")
            return false
        }
        return true
    }

    private fun checkDap(cb: BaseCallback): Boolean {
        if (!Validate.validateDapContract(dapContract).valid) {
            cb.onError("Invalid DAP Contract. Please check that a valid DAP Contract was set before this call.")
            return false
        }
        return true
    }

    /**
     * Creates a blockchain user
     * @param userName
     * @param pubKey
     * @param cb callback, instance of {@link CreateUserCallback}
     */
    fun createUser(userName: String, pubKey: String, cb: CreateUserCallback) {
        //TODO: Receive privateKey and derive pubKey from it instead of receiving pubKey directly
        //TODO: Add sig meta: sig is a sign of the id using the privkey
        val subTx = SubTx(PVER, userName, pubKey)
        val subTxJSON  = createJSONContainer("subtx", JSONObject(gson.toJson(subTx)))
        val userId = Object.setID(subTxJSON)
        val valid = Validate.validateSubTx(subTxJSON)
        val body = JsonParser().parse(subTxJSON.toString()).asJsonObject
        if (valid.valid) {
            dapiService.createUser(body).enqueue({ response ->
                if (response.isSuccessful) {
                    cb.onSuccess(response.body()!!.txId, userId)
                    getUser(userId, object : GetUserCallback {
                        override fun onSuccess(blockchainUserContainer: BlockchainUserContainer) {
                            currentUser = blockchainUserContainer.blockchainuser
                        }

                        override fun onError(errorMessage: String) {

                        }
                    })
                } else {
                    cb.onError(response.message())
                }
            }, { throwable ->
                cb.onError(throwable.localizedMessage)
            })
        } else {
            cb.onError(valid.errMsg!!)
        }
    }

    /**
     * Search for users based on username
     * @param query username or part of it
     * @param cb callback, instance of {@link SearchUsersCallback}
     */
    fun searchUsers(query: String, cb: SearchUsersCallback) {
        dapiService.searchUsers(query).enqueue({ response ->
            if (response.isSuccessful) {
                cb.onSuccess(response.body()!!)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    /**
     * Get User by username or id
     * @param idOrUsername
     * @param cb callback, instance of {@link GetUserCallback}
     */
    fun getUser(idOrUsername: String, cb: GetUserCallback) {
        dapiService.getUser(idOrUsername).enqueue({response ->
            if (response.isSuccessful) {
                //TODO: Remove and creat a login/init method
                currentUser = response.body()!!.blockchainuser
                cb.onSuccess(response.body()!!)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    /**
     * Create a DAP
     * @param dapSchema Schema for the DAP being Created
     * @param userId id of the blockchainuser creating it
     * @param cb callback, instance of {@link PostDapCallback}
     */
    fun createDap(dapSchema: JSONObject, userId: String, cb: PostDapCallback) {
        val dapContract = Create.createDapContract(dapSchema)

        val stPacket = Create.createSTPacketInstance()
        //TODO: Find a better way to access/set properties without dealing with JSON keys directly (?)
        stPacket.getJSONObject(Object.STPACKET).put("dapcontract", dapContract.getJSONObject("dapcontract"))
        /*stPacket.getJSONObject(Object.STPACKET).put("dapid", dapContract.getJSONObject("dapcontract")
                .getJSONObject("meta").getString("id"))*/
        Object.setID(stPacket, dapSchema) //TODO without dapSchema on JS Lib (?)

        val stPacketIsValid = Validate.validateSTPacket(stPacket)
        if (!stPacketIsValid.valid) {
            cb.onError(stPacketIsValid.errMsg!!)
            return
        }

        val pakId = stPacket.getJSONObject(Object.STPACKET).getJSONObject("meta").getString("id")
        val stHeader = Create.createSTHeaderInstance(pakId, userId)
        val dapId = Object.setID(stHeader)

        val stHeaderIsValid = Validate.validateSTHeader(stHeader)
        if (!stHeaderIsValid.valid) {
            cb.onError(stHeaderIsValid.errMsg!!)
            return
        }

        val dap = JSONObject()
        dap.put("ts", stHeader)
        dap.put("tsp", stPacket)
        println(dap)
        dapiService.postDap(JsonParser().parse(dap.toString()).asJsonObject).enqueue({response ->
            if (response.isSuccessful) {
                //TODO: Check if we need to fetch DAP Contract or if just to assign it is ok
                this@DapiClient.dapContract = dapContract
                cb.onSuccess(dapId, response.body()!!.txId)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    /**
     * Commit a Single Object update to a DAP Space
     * @param schemaObject object containing the update
     * @param cb callback, instance of {@link PostDapCallback}
     */
    fun commitSingleObject(schemaObject: JSONObject, cb: PostDapCallback) {
        if (!(checkAuth(cb) && checkDap(cb))) {
            return
        }

        //Create a packet:
        val stPacketContainer = Create.createSTPacketInstance()
        val stPacket = stPacketContainer.getJSONObject(Object.STPACKET)
        val tsp = JSONObject()
        tsp.put(Object.STPACKET, stPacketContainer)

        val dapId = getDapContractId()

        val dapObjects = JSONArray()
        dapObjects.put(schemaObject)

        stPacket.put(Object.DAPOBJECTS, dapObjects)
        stPacket.put("dapobjmerkleroot", "")
        stPacket.put("dapid", dapId)
        val stPacketId = Object.setID(stPacket, getDapContractSchema())

        val validStp = Validate.validateSTPacket(stPacketContainer, getDapContractSchema())
        if (!validStp.valid) {
            cb.onError("Invalid STPacket: $stPacket")
            return
        }

        val ts = Create.createSTHeaderInstance(stPacketId, currentUser!!.uid)
        Object.setID(ts)

        val validTs = Validate.validateSTHeader(ts)
        if (!validTs.valid) {
            cb.onError("Invalid STHeader: $ts")
            return
        }

        val dap = JSONObject()
        dap.put("ts", ts)
        dap.put("tsp", stPacketContainer)

        dapiService.postDap(JsonParser().parse(dap.toString()).asJsonObject).enqueue({response ->
            if (response.isSuccessful) {
                cb.onSuccess(dapId!!, response.body()!!.txId)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    fun addObject(schemaObject: JSONObject, cb: PostDapCallback) {
        schemaObject.put("act", CREATE_OBJECT_ACTION)
        schemaObject.put("rev", 0)

        // get the max idx
        // TODO: account for dapContext download in progress
        val idx = dapContext.optInt("maxidx", 0)
        schemaObject.put("idx", idx)

        commitSingleObject(schemaObject, cb)
    }

    fun updateObject(schemaObject: JSONObject, cb: PostDapCallback) {
        schemaObject.put("act", UPDATE_OBJECT_ACTION)
        schemaObject.put("rev", schemaObject.getInt("rev") + 1)

        commitSingleObject(schemaObject, cb)
    }

    fun removeObject(schemaObject: JSONObject, cb: PostDapCallback) {
        schemaObject.put("act", REMOVE_OBJECT_ACTION)
        schemaObject.put("rev", schemaObject.getInt("rev") + 1)
        schemaObject.put("hdextpubkey", "")

        commitSingleObject(schemaObject, cb)
    }

}