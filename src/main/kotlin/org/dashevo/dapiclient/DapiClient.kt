/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient

import com.google.common.base.Preconditions
import com.google.common.base.Stopwatch
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusRuntimeException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.bitcoinj.evolution.SimplifiedMasternodeListManager
import org.dash.platform.dapi.v0.CoreOuterClass
import org.dash.platform.dapi.v0.PlatformOuterClass
import org.dashevo.dapiclient.grpc.*
import org.dashevo.dapiclient.model.*
import org.dashevo.dapiclient.provider.*
import org.dashevo.dapiclient.rest.DapiService
import org.dashevo.dpp.statetransition.StateTransition
import org.dashevo.dpp.statetransition.StateTransitionIdentitySigned
import org.dashevo.dpp.toBase58
import org.dashevo.dpp.toHexString
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.*


class DapiClient(var dapiAddressListProvider: DAPIAddressListProvider,
                 private var timeOut: Long = DEFAULT_TIMEOUT,
                 private var retries: Int = DEFAULT_RETRY_COUNT,
                 private var banBaseTime: Int = DEFAULT_BASE_BAN_TIME,
                 private var waitForNodes: Int = DEFAULT_WAIT_FOR_NODES) {

    // gRPC properties
    var lastUsedAddress: DAPIAddress? = null

    // jRPC Properties
    private lateinit var retrofit: Retrofit
    private lateinit var dapiService: DapiService
    private val debugOkHttpClient: OkHttpClient
    private val debugJrpc = true
    private var initializedJRPC = false
    private val defaultShouldRetryCallback = DefaultShouldRetryCallback()

    // used for reporting
    private var successfulCalls:Long = 0
    private var failedCalls: Long = 0
    private var totalCalls: Long = 0
    private var retriedCalls: Long = 0
    private val stopWatch = Stopwatch.createStarted()

    // Constants
    companion object {
        private val logger = LoggerFactory.getLogger(DapiClient::class.java.name)
        const val DEFAULT_GRPC_PORT = 3010
        const val DEFAULT_JRPC_PORT = 3000

        const val BLOCK_HASH_LENGTH = 64 // length of a hex string of a hash

        const val DEFAULT_BASE_BAN_TIME = 60 * 1000 // 1 minute

        const val DEFAULT_RETRY_COUNT = 10
        const val USE_DEFAULT_RETRY_COUNT = -1
        const val DEFAULT_TIMEOUT = 5000L  //normally a timeout is 5 seconds longer than this
        const val DEFAULT_WAIT_FOR_NODES = 5
    }

    init {
        val loggingInterceptor = HttpLoggingInterceptor { msg: String? -> logger.info(msg) }
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        debugOkHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

        if (banBaseTime != DEFAULT_BASE_BAN_TIME)
            this.dapiAddressListProvider.setBanBaseTime(banBaseTime)
    }

    constructor(masternodeAddress: String, timeOut: Long = DEFAULT_TIMEOUT, retries: Int = DEFAULT_RETRY_COUNT, banBaseTime: Int = DEFAULT_BASE_BAN_TIME, waitForNodes: Int = DEFAULT_WAIT_FOR_NODES) :
            this(listOf(masternodeAddress), timeOut, retries, banBaseTime, waitForNodes)

    constructor(addresses: List<String>, timeOut: Long = DEFAULT_TIMEOUT, retries: Int = DEFAULT_RETRY_COUNT, banBaseTime: Int = DEFAULT_BASE_BAN_TIME, waitForNodes: Int = DEFAULT_WAIT_FOR_NODES) :
            this(ListDAPIAddressProvider.fromList(addresses, banBaseTime))
    /* Platform gRPC methods */

    /**
     * Broadcast State Transition to machine
     *
     * @param stateTransition
     * @param statusCheck Whether to call getStatus on a node before broadcastStateTransition to avoid usign a bad node
     * @param retryCallback Determines if the broadcast shoudl be tried again after a failure
     */
    fun broadcastStateTransition(stateTransition: StateTransition, statusCheck: Boolean = true, retryCallback: GrpcMethodShouldRetryCallback = DefaultShouldRetryCallback()) {
        logger.info("broadcastStateTransition(${stateTransition.toJSON()})")
        val method = BroadcastStateTransitionMethod(stateTransition)
        grpcRequest(method, statusCheck = statusCheck, retryCallback = retryCallback)
    }

    fun broadcastStateTransitionInternal(stateTransition: StateTransition,
                                         statusCheck: Boolean = true,
                                         retryCallback: GrpcMethodShouldRetryCallback = DefaultShouldRetryCallback())
    : BroadcastStateTransitionMethod {
        logger.info("broadcastStateTransition(${stateTransition.toJSON()})")
        val method = BroadcastStateTransitionMethod(stateTransition)
        grpcRequest(method, statusCheck = statusCheck, retryCallback = retryCallback)
        return method
    }

    /**
     * Wait for state transition result
     * @param hash
     * @param prove
     */
    fun waitForStateTransitionResult(hash: ByteArray, prove: Boolean): WaitForStateTransitionResult {
        val method = WaitForStateTransitionResultMethod(hash, prove)
        logger.info(method.toString())
        val result = grpcRequest(method) as PlatformOuterClass.WaitForStateTransitionResultResponse

        return if (result.hasError())
            WaitForStateTransitionResult(StateTransitionBroadcastException(result.error))
        else WaitForStateTransitionResult(Proof(result.proof))
    }

    val threadPoolService = Executors.newCachedThreadPool()

    inner class WaitForStateSubmittionCallable(val signedStateTransition: StateTransitionIdentitySigned, val prove: Boolean) :
        Callable<WaitForStateTransitionResult> {
        override fun call(): WaitForStateTransitionResult {
            return try {
                waitForStateTransitionResult(signedStateTransition.hashOnce(), prove)
            } catch (e: StatusRuntimeException) {
                WaitForStateTransitionResult(StateTransitionBroadcastException(e.status.code.value(), e.message?:"", ByteArray(0)))
            }
        }
    }

    fun broadcastStateTransitionAndWait(signedStateTransition: StateTransitionIdentitySigned,
                                        retriesLeft: Int = USE_DEFAULT_RETRY_COUNT,
                                        statusCheck: Boolean = true,
                                        retryCallback: BroadcastShouldRetryCallback = DefaultBroadcastRetryCallback()) {

        val retryAttemptsLeft = if (retriesLeft == USE_DEFAULT_RETRY_COUNT) {
            retries // set in constructor
        } else {
            retriesLeft // if called recursively
        }

        val futuresList = arrayListOf<Future<WaitForStateTransitionResult>>()

        val futureWithProof = threadPoolService.submit(WaitForStateSubmittionCallable(signedStateTransition, true))
        futuresList.add(futureWithProof)

        for (i in 0 until waitForNodes - 1)
            futuresList.add(threadPoolService.submit(WaitForStateSubmittionCallable(signedStateTransition, false)))



        var broadcast: BroadcastStateTransitionMethod? = null
        try {
            broadcast = broadcastStateTransitionInternal(signedStateTransition, statusCheck)
        } catch (e: StatusRuntimeException) {
            //should we retry
            logger.info("broadcastStateTransition: failure: $e")
            // cancel all waiting futures
            futuresList.forEach { it.cancel(true) }
            if(!retryCallback.shouldRetry(broadcast!!, e)) {
                //what should we do
                logger.info("Will not retry for $e")
                throw e
            }
            broadcastStateTransitionAndWait(signedStateTransition, retryAttemptsLeft - 1, statusCheck, retryCallback)
        }

        for (future in futuresList)
            future.get(80, TimeUnit.SECONDS)

        val waitForResult = futureWithProof.get()

        val successRate = futuresList.count { it.get().isSuccess() }.toDouble() / futuresList.size

        when {
            successRate > 0.51 -> logger.info("broadcastStateTransition: success ($successRate): ${waitForResult.proof}")
            waitForResult.isError() -> {
                logger.info("broadcastStateTransition: failure: ${waitForResult.error}")
                if(!retryCallback.shouldRetry(broadcast!!, waitForResult.error!!)) {
                    throw waitForResult.error
                }
                broadcastStateTransitionAndWait(signedStateTransition, retryAttemptsLeft - 1, statusCheck, retryCallback)
            }
            successRate <= 0.50 -> {
                logger.info("broadcastStateTransition: failure($successRate): ${waitForResult.error}")
                Thread.sleep(3000)
                // what do we do here?
                if(!retryCallback.shouldRetry(broadcast!!, waitForResult.error!!)) {
                    throw waitForResult.error
                }
                //call wait functions only, not sure if this will work
            }
        }
    }


    /**
     * Fetch the identity by id
     * @param id String
     * @return ByteString?
     */
    fun getIdentity(id: ByteArray, retryCallback: GrpcMethodShouldRetryCallback = defaultShouldRetryCallback): ByteString? {
        logger.info("getIdentity(${id.toBase58()})")
        val method = GetIdentityMethod(id)
        val response = grpcRequest(method, retryCallback = retryCallback) as PlatformOuterClass.GetIdentityResponse?
        return response?.identity
    }

    /**
     * Fetch the identity by the first public key hash
     * @param pubKeyHash ByteArray
     * @return ByteString?
     */
    fun getIdentityByFirstPublicKey(pubKeyHash: ByteArray): ByteString? {
        logger.info("getIdentityByFirstPublicKey(${pubKeyHash.toHexString()})")
        val method = GetIdentitiesByPublicKeyHashes(listOf(pubKeyHash))
        val response = grpcRequest(method) as PlatformOuterClass.GetIdentitiesByPublicKeyHashesResponse?
        val firstResult = response?.identitiesList?.get(0)
        return if (firstResult != null && !firstResult.isEmpty) {
            firstResult
        } else {
            null
        }
    }

    /**
     * Fetch the identity ids by the public key hashes
     * @param pubKeyHashes List<ByteArray>
     * @return List<ByteString>?
     */
    fun getIdentitiesByPublicKeyHashes(pubKeyHashes: List<ByteArray>): List<ByteString>? {
        logger.info("getIdentitiesByPublicKeyHashes(${pubKeyHashes.map { it.toHexString() }}")
        val method = GetIdentitiesByPublicKeyHashes(pubKeyHashes)
        val response = grpcRequest(method) as PlatformOuterClass.GetIdentitiesByPublicKeyHashesResponse?
        return if (response != null && response.identitiesCount > 0 && !response.identitiesList[0].isEmpty) {
            response.identitiesList
        } else {
            null
        }
    }

    /**
     * Fetch the identity id by the first public key hash
     * @param pubKeyHash ByteArray
     * @return String
     */
    fun getIdentityIdByFirstPublicKey(pubKeyHash: ByteArray): ByteString? {
        logger.info("getIdentityIdByFirstPublicKey(${pubKeyHash.toHexString()})")
        val method = GetIdentityIdsByPublicKeyHashes(listOf(pubKeyHash))
        val response = grpcRequest(method) as PlatformOuterClass.GetIdentityIdsByPublicKeyHashesResponse?
        val firstResult = response?.identityIdsList?.get(0)
        return if (firstResult != null && !firstResult.isEmpty) {
            firstResult
        } else {
            null
        }
    }

    /**
     * Fetch the identity ids by the public key hashes
     * @param pubKeyHashes List<ByteArray>
     * @return List<ByteString>?
     */
    fun getIdentityIdsByPublicKeyHashes(pubKeyHashes: List<ByteArray>): List<ByteString>? {
        logger.info("getIdentityIdsByPublicKeyHashes(${pubKeyHashes.map { it.toHexString() }}")
        val method = GetIdentityIdsByPublicKeyHashes(pubKeyHashes)
        val response = grpcRequest(method) as PlatformOuterClass.GetIdentityIdsByPublicKeyHashesResponse?
        return if (response != null && response.identityIdsCount > 0 && !response.identityIdsList[0].isEmpty) {
            response.identityIdsList
        } else {
            null
        }
    }

    /**
     * Fetch Data Contract by id
     * @param contractId String
     * @return ByteString? The contract bytes or null if not found
     */
    fun getDataContract(contractId: ByteArray, retryCallback: GrpcMethodShouldRetryCallback = defaultShouldRetryCallback): ByteString? {
        logger.info("getDataContract(${contractId.toBase58()})")
        val method = GetContractMethod(contractId)
        val response = grpcRequest(method, retryCallback = retryCallback) as PlatformOuterClass.GetDataContractResponse?
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
    fun getDocuments(contractId: ByteArray, type: String, documentQuery: DocumentQuery, retryCallback: GrpcMethodShouldRetryCallback = DefaultGetDocumentsRetryCallback() ): List<ByteArray> {
        logger.info("getDocuments(${contractId.toBase58()}, $type, ${documentQuery.toJSON()})")
        val method = GetDocumentsMethod(contractId, type, documentQuery)
        val response = grpcRequest(method, retryCallback = retryCallback) as PlatformOuterClass.GetDocumentsResponse

        return response.documentsList.map { it.toByteArray() }
    }

    /* Core */
    /** get status of platform node  */
    fun getStatus(address: DAPIAddress? = null, retries: Int = USE_DEFAULT_RETRY_COUNT): GetStatusResponse? {
        val method = GetStatusMethod()
        val watch = Stopwatch.createStarted()
        val response = grpcRequest(method, retries, address) as CoreOuterClass.GetStatusResponse?
        watch.stop()

        return response?.let {
            val result = GetStatusResponse(it.coreVersion,
                    it.protocolVersion,
                    it.blocks,
                    it.timeOffset,
                    it.connections,
                    it.proxy,
                    it.difficulty,
                    it.testnet,
                    it.relayFee,
                    it.errors,
                    it.network,
                    Date().time,
                    address,
                    watch.elapsed(TimeUnit.MILLISECONDS))

            if (address != null)
                address.lastStatus = result

            logger.info("$result")
            result
        }
    }

    private fun logException(e: StatusRuntimeException, masternode: DAPIGrpcMasternode, method: GrpcMethod) {
        logger.warn("RPC failed with ${masternode.address.host}: ${e.status}: ${e.trailers}")
        logger.warn("  for $method")
    }

    fun getBlockByHeight(height: Int): ByteArray? {
        logger.info("getBlockByHeight($height)")
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
        logger.info("getBlockByHash($hash)")
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
        logger.info("getEstimatedTransactionFee($blocks)")
        val method = GetEstimatedTransactionFeeMethod(blocks)
        val response = grpcRequest(method) as CoreOuterClass.GetEstimatedTransactionFeeResponse?
        return response?.fee!!
    }

    /**
     * Make a DAPI call with retry support
     *
     * @param grpcMethod GrpcMethod
     * @param retriesLeft Int The number of times to retry the DAPI call.  (Default = -1, use this.retries)
     * @param dapiAddress DAPIAddress? The node that should used (default = null, choose randomly)
     * @param statusCheck Boolean Should call getStatus on a node before making the DAPI call (default = false)
     * @param retryCallback GrpcMethodShouldRetryCallback Is used upon failure to determine if the DAPI should
     *                      be attempted again after failure
     * @return Any? The result of the call, which must be cast to the correct type by the caller
     */
    private fun grpcRequest(grpcMethod: GrpcMethod,
                            retriesLeft: Int = USE_DEFAULT_RETRY_COUNT,
                            dapiAddress: DAPIAddress? = null,
                            statusCheck: Boolean = false,
                            retryCallback: GrpcMethodShouldRetryCallback = defaultShouldRetryCallback): Any? {
        logger.info("grpcRequest(${grpcMethod.javaClass.simpleName}, $retriesLeft, $dapiAddress, $statusCheck) for $grpcMethod")
        totalCalls++
        val retryAttemptsLeft = if (retriesLeft == USE_DEFAULT_RETRY_COUNT) {
            retries // set in constructor
        } else {
            retriesLeft // if called recursively
        }
        val address = dapiAddress ?: dapiAddressListProvider.getLiveAddress()
        val grpcMasternode = DAPIGrpcMasternode(address, timeOut)
        lastUsedAddress = address

        val response: Any = try {
            if (statusCheck) {
                try {
                    val status = getStatus(grpcMasternode.address, 0)

                    if (status == null) {
                        // throw exception and try another node
                        throw StatusRuntimeException(Status.UNAVAILABLE)
                    } else if (status.errors.isNotEmpty() &&
                            !status.errors.contains("pre-release") &&
                            !status.errors.contains("Warning")) {
                        // see github.com/dashpay/dash/src/warnings.cpp
                        logger.warn("${grpcMasternode.address} has this error state ${status.errors}")
                        // throw exception and try another node
                        throw StatusRuntimeException(Status.UNAVAILABLE)
                    }
                } catch (e: StatusRuntimeException) {
                    throwExceptionOnError(e)

                    banMasternode(grpcMasternode.address, retriesLeft, e)
                    //try another node
                    retriedCalls++
                    return grpcRequest(grpcMethod, retriesLeft, null, statusCheck, retryCallback)
                } catch (e: MaxRetriesReachedException) {
                    banMasternode(grpcMasternode.address, retriesLeft, e.cause as StatusRuntimeException)
                    //try another node
                    retriedCalls++
                    return grpcRequest(grpcMethod, retriesLeft, null, statusCheck, retryCallback)
                }
            }
            logger.debug("grpcMethod: executing method after statuscheck($statusCheck) for $grpcMethod")
            val response = grpcMethod.execute(grpcMasternode)
            successfulCalls++
            response
        } catch (e: StatusRuntimeException) {
            logException(e, grpcMasternode, grpcMethod)
            return if (e.status.code == Status.NOT_FOUND.code) {
                if (!retryCallback.shouldRetry(grpcMethod, e)) {
                    return null
                }

                // only ban the node if the retry == true, meaning that the node
                // returned an untrustworthy NOT_FOUND result
                banMasternode(address, retryAttemptsLeft, e)
                retriedCalls++
                grpcRequest(grpcMethod, retryAttemptsLeft - 1, dapiAddress, statusCheck, retryCallback)
            } else {
                throwExceptionOnError(e, retryCallback)

                banMasternode(address, retryAttemptsLeft, e)
                if (!retryCallback.shouldRetry(grpcMethod, e)) {
                    return null
                }
                retriedCalls++
                grpcRequest(grpcMethod, retryAttemptsLeft - 1, dapiAddress, statusCheck, retryCallback)
            }
        } finally {
            grpcMasternode.shutdown()
        }

        address.markAsLive()
        return response
    }

    private fun banMasternode(
        address: DAPIAddress,
        retryAttemptsLeft: Int,
        e: StatusRuntimeException
    ) {
        logger.info("banning masternode $address")
        failedCalls++
        address.markAsBanned()
        if (retryAttemptsLeft == 0) {
            throw MaxRetriesReachedException(e)
        }
        if (!dapiAddressListProvider.hasLiveAddresses()) {
            throw NoAvailableAddressesForRetryException(e)
        }
    }

    private fun throwExceptionOnError(e: StatusRuntimeException, retryCallback: GrpcMethodShouldRetryCallback? = null) {
        if (retryCallback != null) {
            if(retryCallback.shouldThrowException(e))
                throw e
        } else {
            if (e.status.code != Status.DEADLINE_EXCEEDED.code
                    && e.status.code != Status.UNAVAILABLE.code
                    && e.status.code != Status.INTERNAL.code
                    && e.status.code != Status.CANCELLED.code
                    && e.status.code != Status.UNKNOWN.code) {
                throw e
            }
        }
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
        logger.info("getTransaction($txHex)")
        val method = GetTransactionMethod(txHex)
        val response = grpcRequest(method) as CoreOuterClass.GetTransactionResponse?
        return response?.transaction
    }
    // jRPC methods
    /**
     * Get the best block hash (tip of blockchain)
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

    /**
     * Get the block hash at the specified height
     * @return String?
     */
    fun getBlockHash(height: Int): String? {
        logger.info("getBlockHash(): jRPC")
        val service = getJRPCService()
        val parameters = mapOf("height" to height)
        val response = service.getBlockHash(JsonRPCRequest("getBlockHash", parameters)).execute()
        if (response.isSuccessful) {
            return response.body()!!.result
        } else {
            throw Exception("jRPC error code: ${response.code()})")
        }
    }

    /**
     * Get the masternode list difference between two block hashes
     * @return String?
     */
    fun getMnListDiff(baseBlockHash: String, blockHash: String): Map<String, Any>? {
        logger.info("getMnListDiff(): jRPC")
        val service = getJRPCService()
        val parameters = mapOf(
                "baseBlockHash" to baseBlockHash,
                "blockHash" to blockHash
        )
        val response = service.getMnListDiff(JsonRPCRequest("getMnListDiff", parameters)).execute()
        if (response.isSuccessful) {
            return response.body()!!.result
        } else {
            throw Exception("jRPC error code: ${response.code()})")
        }
    }

    // Internal Methods

    private fun getJRPCService(): DapiService {
        if (initializedJRPC)
            return dapiService

        val mnIP = dapiAddressListProvider.getLiveAddress().host

        logger.info("Connecting to GRPC host: $mnIP:$DEFAULT_JRPC_PORT")

        retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://$mnIP:$DEFAULT_JRPC_PORT/")
                .client(if (debugJrpc) debugOkHttpClient else OkHttpClient())
                .build()
        dapiService = retrofit.create(DapiService::class.java)

        return dapiService
    }

    fun extractException(exception: StatusRuntimeException): List<Map<String, Any>> {
        val trailers = GrpcExceptionInfo(exception)
        return trailers.errors
    }

    fun setSimplifiedMasternodeListManager(simplifiedMasternodeListManager: SimplifiedMasternodeListManager, defaultList: List<String>) {
        dapiAddressListProvider = SimplifiedMasternodeListDAPIAddressProvider(
                simplifiedMasternodeListManager,
                ListDAPIAddressProvider.fromList(defaultList, DEFAULT_BASE_BAN_TIME)
        )
    }

    fun reportNetworkStatus() : String {
        return "DapiClient Network Status\n" +
                "---DAPI Call Statistics ($stopWatch)\n" +
                "   successful: $successfulCalls\n" +
                "   retried   : $retriedCalls\n" +
                "   failure   : $failedCalls\n" +
                "   total     : $totalCalls (calls per minute: ${totalCalls.toDouble()/stopWatch.elapsed(TimeUnit.MINUTES).toDouble()}\n" +
                "   retried % : ${retriedCalls.toDouble()/successfulCalls.toDouble()*100}%\n" +
                "   success % : ${successfulCalls.toDouble()/totalCalls.toDouble()*100}%\n" +
                "---Masternode Information\n" + dapiAddressListProvider.getStatistics()

    }
}
