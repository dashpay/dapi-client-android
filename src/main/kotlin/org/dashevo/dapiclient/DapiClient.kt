/**
 * Copyright (c) 2018-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.objectbox.BoxStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.dashevo.dapiclient.callback.BaseCallback
import org.dashevo.dapiclient.callback.BaseTxCallback
import org.dashevo.dapiclient.callback.ErrorCallback
import org.dashevo.dapiclient.extensions.enqueue
import org.dashevo.dapiclient.model.*
import org.dashevo.dapiclient.persistance.PersistenceInterceptor
import org.dashevo.dapiclient.persistance.model.MyObjectBox
import org.dashevo.dapiclient.rest.DapiService
import org.dashevo.schema.Create
import org.dashevo.schema.Object
import org.dashevo.schema.Validate
import org.jsonorg.JSONArray
import org.jsonorg.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

open class DapiClient(mnIP: String, mnDapiPort: String, debug: Boolean = false) {

    private val retrofit: Retrofit
    private val gson = Gson()

    val dapiService: DapiService

    var dapContext: DapContext? = null
    var dapSpace: DapSpace? = null
    var currentUser: BlockchainUser? = null
    var dapContract = JSONObject()

    val persistenceInterceptor: PersistenceInterceptor
    lateinit var boxStore: BoxStore

    companion object {
        private const val PVER = 1
        private const val DATABASE_NAME = "objectbox-cache-db"
    }

    init {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        persistenceInterceptor = PersistenceInterceptor()

        val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(persistenceInterceptor)
                .build()
        val debugOkHttpClient = OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(persistenceInterceptor)
                .build()
        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://$mnIP:$mnDapiPort/")
                .client(if (debug) debugOkHttpClient else okHttpClient)
                .build()
        dapiService = retrofit.create(DapiService::class.java)
    }

    fun enablePersistence(baseDirectory: File) {
        boxStore = MyObjectBox.builder().baseDirectory(baseDirectory).name(DATABASE_NAME).build()
        persistenceInterceptor.boxStore = boxStore
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

    /**
     * Check if [currentUser] is set.
     */
    open fun checkAuth(cb: ErrorCallback? = null): Boolean {
        if (currentUser?.buid == null) {
            cb?.onError("User session null or invalid")
            return false
        }
        return true
    }

    /**
     * Check if [dapContract] is set.
     */
    open fun checkDap(cb: ErrorCallback? = null): Boolean {
        if (!Validate.validateDapContract(dapContract).valid) {
            cb?.onError("Invalid DAP Contract. Please check that a valid DAP Contract was set before this call.")
            return false
        }
        return true
    }

    /**
     * Creates a blockchain user
     * @param userName
     * @param pubKey
     * @param cb callback, instance of {@link BaseTxCallback<String, String>}
     */
    open fun createUser(userName: String, pubKey: String, cb: BaseTxCallback<String, String>) {
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
     * @param cb [BaseCallback<BlockchainUser>]
     */
    open fun login(username: String, cb: BaseCallback<BlockchainUser>) {
        getUser(username, object : BaseCallback<BlockchainUserContainer> {
            override fun onSuccess(blockchainUserContainer: BlockchainUserContainer) {
                val blockchainUser = blockchainUserContainer.blockchainuser
                currentUser = blockchainUser

                val valid = Validate.validateBlockchainUser(JSONObject(gson.toJson(blockchainUserContainer)))
                if (!valid.valid) {
                    cb.onError(valid.errMsg!!)
                    return
                }

                if (!checkDap()) {
                    cb.onSuccess(blockchainUser)
                    return
                }

                dapiService.getDapSpace(getDapContractId()!!, blockchainUser.buid).enqueue({
                    if (it.isSuccessful) {
                        dapSpace = it.body()
                    } else {
                        println("Login successful but failed to get user DAP Space")
                    }
                    cb.onSuccess(blockchainUser)
                }, {
                    println("Login successful but failed to get user DAP Space")
                    cb.onSuccess(blockchainUser)
                })
            }

            override fun onError(errorMessage: String) {
                cb.onError(errorMessage)
            }
        })
    }

    /**
     * Logout, reset [currentUser] and [dapContext]
     */
    open fun logout() {
        currentUser = null
        dapContext = null
    }

    /**
     * Search for users based on username
     * @param query username or part of it
     * @param cb callback, instance of {@link BaseCallback<List<BlockchainUserContainer>>}
     */
    open fun searchUsers(query: String, cb: BaseCallback<List<BlockchainUserContainer>>) {
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
     * @param cb callback, instance of {@link BaseCallback<BlockchainUserContainer>}
     */
    open fun getUser(idOrUsername: String, cb: BaseCallback<BlockchainUserContainer>) {
        dapiService.getUser(idOrUsername).enqueue({response ->
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
     * Create a DAP
     * @param dapSchema Schema for the DAP being Created
     * @param userId id of the blockchainuser creating it
     * @param cb callback, instance of {@link BaseCallback<String>}
     */
    open fun createDap(dapSchema: JSONObject, userId: String, cb: BaseCallback<String>) {
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
    open fun getDap(dapId: String, cb: BaseCallback<String>) {
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
     * @param cb callback, instance of {@link BaseTxCallback<String, String>}
     */
    open fun commitSingleObject(schemaObject: JSONObject, cb: BaseTxCallback<String, String>) {
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
                dapiService.getDapSpace(dapId!!, currentUser!!.buid).enqueue({
                    if (response.isSuccessful) {
                        this.dapSpace = it.body()
                        cb.onSuccess(dapId, response.body()!!.txId)
                    } else {
                        cb.onError(response.message())
                    }
                }, {
                    cb.onError(it.localizedMessage)
                })
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
    open fun getDapSpace(cb: BaseCallback<DapSpace>) {
        if (!(checkAuth(cb) && checkDap(cb))) {
            return
        }

        val dapId = getDapContractId()!!
        val buId = currentUser!!.buid

        dapiService.getDapSpace(dapId, buId).enqueue({ response ->
            val dapSpaceResponse: DapSpace? = response.body()
            if (response.isSuccessful && dapSpaceResponse != null) {
                dapSpace = dapSpaceResponse
                cb.onSuccess(dapSpaceResponse)
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
    open fun getDapContext(cb: BaseCallback<DapContext>) {
        if (!(checkAuth(cb) && checkDap(cb))) {
            return
        }

        val dapId = getDapContractId()!!
        val buId = currentUser!!.buid

        dapiService.getDapContext(dapId, buId).enqueue({ response ->
            if (response.isSuccessful && response.body() != null) {
                dapContext = response.body()!!
                cb.onSuccess(dapContext!!)
            } else {
                cb.onError(response.message())
            }
        }, { throwable ->
            cb.onError(throwable.localizedMessage)
        })
    }

    open fun addObject(obj: JSONObject, cb: BaseTxCallback<String, String>) {
        val idx = if (this.dapSpace!= null) this.dapSpace!!.maxidx + 1 else 0
        obj.put("idx", idx)
        commitSingleObject(obj, cb)
    }

    open fun removeObject(schemaObject: JSONObject, cb: BaseTxCallback<String, String>) {
        Object.prepareForRemoval(schemaObject)
        commitSingleObject(schemaObject, cb)
    }

}