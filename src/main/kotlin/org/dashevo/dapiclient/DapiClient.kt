/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient

import com.google.common.base.Preconditions
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusRuntimeException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.bitcoinj.evolution.SimplifiedMasternodeListManager
import org.dash.platform.dapi.v0.CoreOuterClass
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import org.dash.platform.dapi.v0.PlatformOuterClass
import org.dashevo.dapiclient.grpc.*
import org.dashevo.dapiclient.model.DocumentQuery
import org.dashevo.dapiclient.model.GetStatusResponse
import org.dashevo.dapiclient.model.JsonRPCRequest
import org.dashevo.dapiclient.provider.*
import org.dashevo.dapiclient.rest.DapiService
import org.dashevo.dpp.statetransition.StateTransition
import org.dashevo.dpp.toHexString
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.logging.Level


class DapiClient(var dapiAddressListProvider: DAPIAddressListProvider,
                 val diffMasternodeEachCall: Boolean = true,
                 val debug: Boolean = false) {

    // gRPC properties
    var lastUsedAddress: DAPIAddress? = null

    // jRPC Properties
    private lateinit var retrofit: Retrofit
    private lateinit var dapiService: DapiService
    private val debugOkHttpClient: OkHttpClient
    private var initializedJRPC = false

    // Constants
    companion object {
        private val logger = Logger.getLogger(DapiClient::class.java.name)
        const val DEFAULT_GRPC_PORT = 3010
        const val DEFAULT_JRPC_PORT = 3000

        const val BLOCK_HASH_LENGTH = 64 // length of a hex string of a hash

        const val BASE_BAN_TIME = 60 * 1000 // 1 minute

        const val DEFAULT_RETRY_COUNT = 5
        const val DEFAULT_TIMEOUT = 5000
    }


    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        debugOkHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

    }

    constructor(masternodeAddress: String, diffMasternodeEachCall: Boolean = true) :
            this(listOf(masternodeAddress), diffMasternodeEachCall)

    constructor(addresses: List<String>, diffMasternodeEachCall: Boolean = true) :
            this(ListDAPIAddressProvider.fromList(addresses, BASE_BAN_TIME), diffMasternodeEachCall)
    /* Platform gRPC methods */

    /**
     * Send State Transition to machine
     *
     * @param stateTransition
     */
    fun broadcastStateTransition(stateTransition: StateTransition) {
        logger.info("broadcastStateTransition(${stateTransition.toJSON()})")
        val method = BroadcastStateTransitionMethod(stateTransition)
        grpcRequest(method)
    }

    /**
     * Fetch the identity by id
     * @param id String
     * @return ByteString?
     */
    fun getIdentity(id: String): ByteString? {
        logger.log(Level.INFO, "getIdentity($id)")
        val method = GetIdentityMethod(id)
        val response = grpcRequest(method) as PlatformOuterClass.GetIdentityResponse?
        return response?.identity
    }

    /**
     * Fetch the identity by the first public key hash
     * @param pubKeyHash ByteArray
     * @return ByteString?
     */
    fun getIdentityByFirstPublicKey(pubKeyHash: ByteArray): ByteString? {
        logger.log(Level.INFO, "getIdentityByFirstPublicKey(${ pubKeyHash.toHexString() })")
        val method = GetIdentityByFirstPublicKeyMethod(pubKeyHash)
        val response = grpcRequest(method) as PlatformOuterClass.GetIdentityByFirstPublicKeyResponse?
        return response?.identity
    }

    /**
     * Fetch the identity id by the first public key hash
     * @param pubKeyHash ByteArray
     * @return String
     */
    fun getIdentityIdByFirstPublicKey(pubKeyHash: ByteArray): String? {
        logger.log(Level.INFO, "getIdentityIdByFirstPublicKey(${ pubKeyHash.toHexString() })")
        val method = GetIdentityIdByFirstPublicKeyMethod(pubKeyHash)
        val response = grpcRequest(method) as PlatformOuterClass.GetIdentityIdByFirstPublicKeyResponse?
        return response?.id
    }

    /**
     * Fetch Data Contract by id
     * @param contractId String
     * @return ByteString? The contract bytes or null if not found
     */
    fun getDataContract(contractId: String): ByteString? {
        logger.log(Level.INFO, "getDataContract($contractId)")
        val method = GetContractMethod(contractId)
        val response = grpcRequest(method) as PlatformOuterClass.GetDataContractResponse?
        return response?.dataContract
    }

    /**
     *
     * @param contractId String The contract id associated with the documents
     * @param type String The type of document
     * @param documentQuery DocumentQuery DocumentQuery that specify which documents to find, sort order
     * and pagination
     * @return List<ByteArray>? a list of documents matching the provided parameters
     */
    fun getDocuments(contractId: String, type: String, documentQuery: DocumentQuery): List<ByteArray>? {
        logger.log(Level.INFO, "getDocuments($contractId, $type, ${documentQuery.toJSON()})")
        val method = GetDocumentsMethod(contractId, type, documentQuery)
        val response = grpcRequest(method) as PlatformOuterClass.GetDocumentsResponse

        return response.documentsList.map { it.toByteArray() }
    }

    /* Core */
    /** get status of platform node  */
    fun getStatus(): GetStatusResponse? {
        val method = GetStatusMethod()
        val response = grpcRequest(method) as CoreOuterClass.GetStatusResponse?

        return response?.let {
            GetStatusResponse(it.coreVersion,
                    it.protocolVersion,
                    it.blocks,
                    it.timeOffset,
                    it.connections,
                    it.proxy,
                    it.difficulty,
                    it.testnet,
                    it.relayFee,
                    it.errors,
                    it.network)
        }
    }

    private fun logException(e: StatusRuntimeException) {
        logger.log(Level.WARNING, "RPC failed: ${e.status}: ${e.trailers}")
    }

    fun getBlockByHeight(height: Int): ByteArray? {
        logger.log(Level.INFO, "getBlockByHeight($height)")
        Preconditions.checkArgument(height > 0)
        val request = CoreOuterClass.GetBlockRequest.newBuilder()
                .setHeight(height)
                .build()
        return getBlock(request)
    }

    /**
     *
     * @param hash String
     * @return ByteArray?
     */

    fun getBlockByHash(hash: String): ByteArray? {
        logger.log(Level.INFO, "getBlockByHash($hash)")
        Preconditions.checkArgument(hash.length == BLOCK_HASH_LENGTH)
        val request = CoreOuterClass.GetBlockRequest.newBuilder()
                .setHash(hash)
                .build()
        return getBlock(request)
    }

    private fun getBlock(request: CoreOuterClass.GetBlockRequest?): ByteArray? {
        val getBlock = GetBlockMethod(request!!)
        val response = grpcRequest(getBlock) as CoreOuterClass.GetBlockResponse?
        return response?.block!!.toByteArray()
    }

    fun getEstimatedTransactionFee(blocks: Int): Double {
        val method = GetEstimatedTransactionFeeMethod(blocks)
        val response = grpcRequest(method) as CoreOuterClass.GetEstimatedTransactionFeeResponse?
        return response?.fee!!
    }

    private fun grpcRequest(grpcMethod: GrpcMethod, retries: Int = DEFAULT_RETRY_COUNT, timeout: Int = DEFAULT_TIMEOUT): Any? {
        logger.info("grpcRequest ${grpcMethod.javaClass.simpleName}")
        val address = dapiAddressListProvider.getLiveAddress()
        val grpcMasternode = DAPIGrpcMasternode(address, timeout)
        lastUsedAddress = address

        val response: Any = try {
            grpcMethod.execute(grpcMasternode)
        } catch (e: StatusRuntimeException) {
            logException(e)
            return if (e.status.code == Status.NOT_FOUND.code) {
                null
            } else {
                if (e.status.code != Status.DEADLINE_EXCEEDED.code
                        && e.status.code != Status.UNAVAILABLE.code
                        && e.status.code != Status.INTERNAL.code
                        && e.status.code != Status.CANCELLED.code
                        && e.status.code != Status.UNKNOWN.code) {
                    throw e
                }
                address.markAsBanned()
                if (retries == 0) {
                    throw MaxRetriesReachedException(e)
                }
                if (!dapiAddressListProvider.hasLiveAddresses()) {
                    throw NoAvailableAddressesForRetryException(e)
                }
                grpcRequest(grpcMethod, retries - 1)
            }
        } finally {
            grpcMasternode.shutdown()
        }

        address.markAsLive()
        return response
    }

    fun broadcastTransaction(txBytes: ByteString, allowHighFees: Boolean = false, bypassLimits: Boolean = false): String {
        val method = BroadcastTransactionMethod(txBytes, allowHighFees, bypassLimits)
        val response = grpcRequest(method) as CoreOuterClass.BroadcastTransactionResponse?
        return response?.transactionId!!
    }

    /**
     *
     * @param txHex String
     * @return ByteString?
     */
    fun getTransaction(txHex: String): ByteString? {
        logger.log(Level.INFO, "getTransaction($txHex)")
        val method = GetTransactionMethod(txHex)
        val response = grpcRequest(method) as CoreOuterClass.GetTransactionResponse?
        return response?.transaction
    }
    // jRPC methods
    /**
     *
     * @return String?
     */
    fun getBestBlockHash(): String? {
        logger.info("getBestBlockHash(): jRPC")
        val service = getJRPCService()
        val response = service.getBestBlockHash(JsonRPCRequest("getBestBlockHash", mapOf())).execute()
        if (response.isSuccessful) {
            return response.body()!!.result
        } else {
            throw Exception("jRPC error code: ${response.code()})")
        }
    }

    // Internal Methods

    private fun getJRPCService(): DapiService {
        if (diffMasternodeEachCall && initializedJRPC)
            return dapiService

        val mnIP = dapiAddressListProvider.getLiveAddress().host

        logger.info("Connecting to GRPC host: $mnIP:$DEFAULT_JRPC_PORT")

        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://$mnIP:$DEFAULT_JRPC_PORT/")
                .client(if (debug) debugOkHttpClient else OkHttpClient())
                .build()
        dapiService = retrofit.create(DapiService::class.java)

        return dapiService
    }

    fun processException(exception: StatusRuntimeException) {
        val x = JSONObject(exception.trailers.toString())
        when (exception.status.code) {
            Status.Code.INVALID_ARGUMENT -> {

            }
        }
    }

    fun setSimplifiedMasternodeListManager(simplifiedMasternodeListManager: SimplifiedMasternodeListManager, defaultList: List<String>) {
        dapiAddressListProvider = SimplifiedMasternodeListDAPIAddressProvider(
                simplifiedMasternodeListManager,
                ListDAPIAddressProvider.fromList(defaultList, BASE_BAN_TIME)
        )
    }
}
