/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import org.dashevo.dapiclient.callback.*
import org.dashevo.dapiclient.extensions.enqueue
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.BlockchainUserContainer
import org.dashevo.dapiclient.model.DapSpace
import org.dashevo.dapiclient.model.SubTx
import org.dashevo.dapiclient.rest.DapiService
import org.dashevo.schema.Create
import org.dashevo.schema.Object
import org.dashevo.schema.Validate
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class DapiClient(mnIP: String, mnDapiPort: String, debug: Boolean = false) {

    private val retrofit: Retrofit
    private val dapiService: DapiService
    private val gson = Gson()
    private var dapContract = JSONObject()
    private var dapContext: DapSpace? = null
    private var currentUser: BlockchainUser? = null

    companion object {
        private const val PVER = 1
    }

    init {
        val debugOkHttpClient = OkHttpClient.Builder()
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
        if (currentUser?.buid == null) {
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
        //TODO: * Add sig meta
        val subTx = SubTx(PVER, userName, pubKey)
        val subTxJSON  = createJSONContainer("subtx", JSONObject(gson.toJson(subTx)))
        val userId = Object.setID(subTxJSON)
        val valid = Validate.validateSubTx(subTxJSON)

        if (!valid.valid) {
            return cb.onError(valid.errMsg!!)
        }

        val body = JsonParser().parse(subTxJSON.toString()).asJsonObject
        dapiService.createUser(body).enqueue({ response ->
            if (response.isSuccessful) {
                cb.onSuccess(response.body()!!.txId, userId)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    /**
     * Fetch user by [username] and the DAP Context for given user if [dapContract] is set.
     * @param cb [LoginCallback]
     */
    fun login(username: String, cb: LoginCallback) {
        getUser(username, object : GetUserCallback {
            override fun onSuccess(blockchainUserContainer: BlockchainUserContainer) {
                val blockchainUser = blockchainUserContainer.blockchainuser
                currentUser = blockchainUser

                val valid = Validate.validateBlockchainUser(JSONObject(gson.toJson(blockchainUserContainer)))
                if (!valid.valid) {
                    cb.onError(valid.errMsg!!)
                    return
                }

                if (!checkDap(cb)) {
                    cb.onSuccess(blockchainUser)
                    return
                }

                try {
                    val dapContextResponse = dapiService.getDapContext(getDapContractId()!!,
                            blockchainUser.buid).execute()
                    if (dapContextResponse.isSuccessful) {
                        dapContext = dapContextResponse.body()
                    }
                } catch (e: Exception) {
                    println("Login successful but failed to get user DAP Context")
                } finally {
                    cb.onSuccess(blockchainUser)
                }
            }

            override fun onError(errorMessage: String) {
                cb.onError(errorMessage)
            }
        })
    }

    /**
     * Logout, reset [currentUser] and [dapContext]
     */
    fun logout() {
        currentUser = null
        dapContext = null
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
     * @param cb callback, instance of {@link DapCallback}
     */
    fun createDap(dapSchema: JSONObject, userId: String, cb: DapCallback) {
        val dapContract = Create.createDapContract(dapSchema)

        val stPacket = Create.createSTPacketInstance()
        stPacket.getJSONObject(Object.STPACKET).put("dapcontract", dapContract.getJSONObject("dapcontract"))
        Object.setID(stPacket, dapSchema)

        val stPacketIsValid = Validate.validateSTPacket(stPacket)
        if (!stPacketIsValid.valid) {
            cb.onError(stPacketIsValid.errMsg!!)
            return
        }

        val stHeader = Create.createSTHeaderInstance(stPacket, userId)
        val stHeaderIsValid = Validate.validateSTHeader(stHeader)
        if (!stHeaderIsValid.valid) {
            cb.onError(stHeaderIsValid.errMsg!!)
            return
        }

        val dap = JSONObject()
        dap.put("ts", stHeader)
        dap.put("tsp", stPacket)

        dapiService.postDap(JsonParser().parse(dap.toString()).asJsonObject).enqueue({ response ->
            if (response.isSuccessful) {
                val txId = response.body()!!.txId
                Object.setMeta(dapContract, "dapid", txId)
                this@DapiClient.dapContract = dapContract
                cb.onSuccess(txId)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    /**
     * Load DAP and assign it to [dapContract]
     */
    fun getDap(dapId: String, cb: DapCallback) {
        dapiService.getDap(dapId).enqueue({ response ->
            if (response.isSuccessful) {
                dapContract = JSONObject(response.body()!!.toString())
                cb.onSuccess(dapId)
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
     * @param cb callback, instance of {@link DapCallback}
     */
    fun commitSingleObject(schemaObject: JSONObject, cb: CommitDapObjectCallback) {
        if (!(checkAuth(cb) && checkDap(cb))) {
            return
        }

        val dapId = getDapContractId()

        //Create a packet:
        val stPacketContainer = Create.createSTPacketInstance()
        val stPacket = stPacketContainer.getJSONObject(Object.STPACKET)

        val tsp = JSONObject()
        tsp.put(Object.STPACKET, stPacketContainer)

        //Add schemaObject to dapobjects array
        val dapObjects = JSONArray()
        dapObjects.put(schemaObject)

        stPacket.put(Object.DAPOBJECTS, dapObjects)
        stPacket.put("dapobjectshash", "")
        stPacket.put("dapid", dapId)
        Object.setID(stPacket, getDapContractSchema())

        val ts = Create.createSTHeaderInstance(stPacketContainer, currentUser!!.buid)
        Object.setID(ts)

        //TODO: * STPacket/STHeader validation seems to have an issue on DashSchema.JS that might be related with how avj on js doesn't fetch $ref schemas
        /*
        val validStp = Validate.validateSTPacket(stPacketContainer, getDapContractSchema())
        if (!validStp.valid) {
            cb.onError("Invalid STPacket: $stPacket")
            return
        }

        val validTs = Validate.validateSTHeader(ts)
        if (!validTs.valid) {
            cb.onError("Invalid STHeader: $ts")
            return
        }
        */

        val dapObject = JSONObject()
        dapObject.put("ts", ts)
        dapObject.put("tsp", stPacketContainer)

        dapiService.postDap(JsonParser().parse(dapObject.toString()).asJsonObject).enqueue({ response ->
            if (response.isSuccessful) {
                cb.onSuccess(dapId!!, response.body()!!.txId)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    /**
     * Get User data in a DAP
     */
    fun getDapSpace(cb: GetDapSpaceCallback) {
        if (!(checkAuth(cb) && checkDap(cb))) {
            return
        }

        val dapId = getDapContractId()!!
        val buId = currentUser!!.buid

        dapiService.getDapSpace(dapId, buId).enqueue({ response ->
            if (response.isSuccessful && response.body() != null) {
                cb.onSuccess(response.body()!!)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    /**
     * Get User data and it's related data in a DAP
     */
    fun getDapContext(cb: GetDapSpaceCallback) {
        if (!(checkAuth(cb) && checkDap(cb))) {
            return
        }

        val dapId = getDapContractId()!!
        val buId = currentUser!!.buid

        dapiService.getDapSpace(dapId, buId).enqueue({ response ->
            if (response.isSuccessful && response.body() != null) {
                cb.onSuccess(response.body()!!)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    fun removeObject(schemaObject: JSONObject, cb: CommitDapObjectCallback) {
        Object.prepareForRemoval(schemaObject)
        commitSingleObject(schemaObject, cb)
    }

}