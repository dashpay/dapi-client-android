/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient

import com.google.common.base.Preconditions
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.dash.platform.dapi.v0.CoreGrpc
import org.dash.platform.dapi.v0.CoreOuterClass
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import org.dash.platform.dapi.v0.PlatformGrpc
import org.dash.platform.dapi.v0.PlatformOuterClass
import org.dashevo.dapiclient.model.DocumentQuery
import org.dashevo.dapiclient.model.GetStatusResponse
import org.dashevo.dapiclient.model.JsonRPCRequest
import org.dashevo.dapiclient.rest.DapiService
import org.dashevo.dpp.statetransition.StateTransition
import org.dashevo.dpp.toHexString
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.logging.Level


class DapiClient(val masternodeService: MasternodeService, val diffMasternodeEachCall: Boolean = true, val debug: Boolean = false) {

    // gRPC properties
    lateinit var channel: ManagedChannel
    lateinit var platform: PlatformGrpc.PlatformBlockingStub
    lateinit var core: CoreGrpc.CoreBlockingStub
    private var initialized = false

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
            this(SingleMasternode(masternodeAddress), diffMasternodeEachCall)

    /* Platform gRPC methods */

    /**
     * Send State Transition to machine
     *
     * @param stateTransition
     */
    fun applyStateTransition(stateTransition: StateTransition) {
        logger.log(Level.INFO, "applyStateTransition()")
        val applyStateTransitionRequest = PlatformOuterClass.ApplyStateTransitionRequest.newBuilder()
                .setStateTransition(ByteString.copyFrom(stateTransition.serialize()))
                .build()

        val service = getPlatformService()

        try {
            service.applyStateTransition(applyStateTransitionRequest)
        } catch (e: StatusRuntimeException) {
            logException(e)
            throw e
        }
    }

    /**
     * Fetch the identity by id
     * @param id String
     * @return ByteString?
     */
    fun getIdentity(id: String): ByteString? {
        logger.log(Level.INFO, "getDataContract($id)")
        val getIdentityRequest = PlatformOuterClass.GetIdentityRequest.newBuilder()
                .setId(id)
                .build()

        val service = getPlatformService()

        val getIdentityResponse: PlatformOuterClass.GetIdentityResponse
        try {
            getIdentityResponse = service.getIdentity(getIdentityRequest)

            val serializedIdentityBinaryArray = getIdentityResponse.identity

            return if (!serializedIdentityBinaryArray.isEmpty)
                serializedIdentityBinaryArray
            else null
        } catch (e: StatusRuntimeException) {
            logException(e)
            if (e.status.code == Status.NOT_FOUND.code) {
                return null
            }
            throw e
        } finally {
            shutdownInternal()
        }
    }

    /**
     * Fetch Data Contract by id
     * @param contractId String
     * @return ByteString? The contract bytes or null if not found
     */
    fun getDataContract(contractId: String): ByteString? {
        logger.log(Level.INFO, "getDataContract($contractId)")

        val getDataContractRequest = PlatformOuterClass.GetDataContractRequest.newBuilder()
                .setId(contractId)
                .build()

        val service = getPlatformService()

        try {
            var getDataContractResponse = service.getDataContract(getDataContractRequest)
            return getDataContractResponse.dataContract ?: return null
        } catch (e: StatusRuntimeException) {
            logException(e)
            if (e.status.code == Status.NOT_FOUND.code) {
                return null
            } else throw e
        } finally {
            shutdownInternal()
        }
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
        val builder = PlatformOuterClass.GetDocumentsRequest.newBuilder()
                .setDataContractId(contractId)
                .setDocumentType(type)
                .setWhere(ByteString.copyFrom(documentQuery.encodeWhere()))
                .setOrderBy(ByteString.copyFrom(documentQuery.encodeOrderBy()))
        if (documentQuery.hasLimit())
            builder.limit = documentQuery.limit
        if (documentQuery.hasStartAfter())
            builder.startAfter = documentQuery.startAfter
        if (documentQuery.hasStartAt())
            builder.startAt = documentQuery.startAt

        val getDocumentsRequest = builder.build()

        val service = getPlatformService()

        try {
            val getDocumentsResponse = service.getDocuments(getDocumentsRequest)

            return getDocumentsResponse.documentsList.map { it.toByteArray() }
        } finally {
            shutdownInternal()
        }
    }

    /* Core */
    /** get status of platform node  */
    fun getStatus(): GetStatusResponse? {
        logger.log(Level.INFO, "getStatus()")
        val request = CoreOuterClass.GetStatusRequest.newBuilder().build()

        val service = getCoreService()

        val response: CoreOuterClass.GetStatusResponse = try {
            service.getStatus(request)
        } catch (e: StatusRuntimeException) {
            logException(e)
            throw e
        } finally {
            shutdownInternal()
        }

        return GetStatusResponse(response.coreVersion,
                response.protocolVersion,
                response.blocks,
                response.timeOffset,
                response.connections,
                response.proxy,
                response.difficulty,
                response.testnet,
                response.relayFee,
                response.errors,
                response.network)
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
        val service = getCoreService()

        val response: CoreOuterClass.GetBlockResponse = try {
            service.getBlock(request)
        } catch (e: StatusRuntimeException) {
            logException(e)
            if (e.status.code == Status.NOT_FOUND.code) {
                return null
            } else throw e
        } finally {
            shutdownInternal()
        }

        return response.block.toByteArray()
    }

    fun sendTransaction(txBytes: ByteString, allowHighFees: Boolean = false, bypassLimits: Boolean = false): String {
        logger.info("sendTransaction(${txBytes.toByteArray().toHexString()}, allowHighFees=$allowHighFees, bypassLimits=$bypassLimits): jRPC")
        val request = CoreOuterClass.SendTransactionRequest.newBuilder()
                .setTransaction(txBytes)
                .setAllowHighFees(allowHighFees)
                .setBypassLimits(bypassLimits)
                .build()
        val service = getCoreService()

        return try {
            val response: CoreOuterClass.SendTransactionResponse = service.sendTransaction(request)
            logger.info("Response: $response")
            response.transactionId
        } catch (e: StatusRuntimeException) {
            logException(e)
            throw e
        } finally {
            shutdownInternal()
        }
    }

    /**
     *
     * @param txHex String
     * @return ByteString?
     */
    fun getTransaction(txHex: String): ByteString? {
        logger.log(Level.INFO, "getTransaction($txHex)")
        val request = CoreOuterClass.GetTransactionRequest.newBuilder()
                .setId(txHex)
                .build()

        val service = getCoreService()

        return try {
            val response: CoreOuterClass.GetTransactionResponse = service.getTransaction(request)
            logger.info("Response: $response")
            response.transaction
        } catch (e: StatusRuntimeException) {
            logException(e)
            if (e.status.code == Status.NOT_FOUND.code) {
                null
            } else throw e
        } finally {
            shutdownInternal()
        }
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

    private fun getGrpcHost(): String {
        return masternodeService.getServer()
    }

    private fun getPlatformService(): PlatformGrpc.PlatformBlockingStub {
        if (diffMasternodeEachCall && initialized)
            return platform

        initializeService()

        return platform
    }

    private fun initializeService() {
        val host = getGrpcHost()
        logger.info("Connecting to GRPC host: $host:$DEFAULT_GRPC_PORT")
        channel = ManagedChannelBuilder.forAddress(host, DEFAULT_GRPC_PORT)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .idleTimeout(5, TimeUnit.SECONDS)
                .build()

        core = CoreGrpc.newBlockingStub(channel)
        platform = PlatformGrpc.newBlockingStub(channel)


        initialized = true
    }

    private fun getCoreService(): CoreGrpc.CoreBlockingStub {
        if (diffMasternodeEachCall && initialized)
            return core

        initializeService()

        return core
    }

    private fun getJRPCService(): DapiService {
        if (diffMasternodeEachCall && initializedJRPC)
            return dapiService

        val mnIP = getGrpcHost()

        logger.info("Connecting to GRPC host: $mnIP:$DEFAULT_JRPC_PORT")

        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://$mnIP:$DEFAULT_JRPC_PORT/")
                .client(if (debug) debugOkHttpClient else OkHttpClient())
                .build()
        dapiService = retrofit.create(DapiService::class.java)

        return dapiService
    }


    fun shutdown() {
        logger.info("shutdown: " + !channel.isShutdown)
        if (!channel.isShutdown) {
            logger.info("Shutting down: " + channel.shutdown())
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun shutdownInternal() {
        logger.info("shutdown:internal: " + (diffMasternodeEachCall && initialized))
        if (diffMasternodeEachCall && initialized) {
            logger.info("Shutting down: " + channel.shutdown())
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
            initialized = false
        }
    }

    fun processException(exception: StatusRuntimeException) {
        val x = JSONObject(exception.trailers.toString())
        when (exception.status.code) {
            Status.Code.INVALID_ARGUMENT -> {

            }
        }
    }
}
