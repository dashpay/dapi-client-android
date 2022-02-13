package org.dashj.platform.dapiclient.grpc

import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import org.bitcoinj.core.BloomFilter
import org.bitcoinj.core.Sha256Hash
import org.dash.platform.dapi.v0.CoreOuterClass
import org.dashj.platform.dapiclient.model.TransactionsWithProofsResponse
import org.dashj.platform.dapiclient.provider.DAPIGrpcMasternode

class SubscribeToTransactionsWithProofsMethod(
    private val bloomFilter: BloomFilter,
    private val fromBlockHash: Sha256Hash,
    private val fromBlockHeight: Int,
    private val count: Int,
    private val sendTransactionHashes: Boolean,
    private val subscribeToTransactions: SubscribeToTransactionsWithProofs
) : GrpcMethod {

    constructor(
        bloomFilter: BloomFilter,
        fromBlockHash: Sha256Hash,
        count: Int,
        sendTransactionHashes: Boolean,
        subscribeToTransactions: SubscribeToTransactionsWithProofs
    ) :
        this(bloomFilter, fromBlockHash, -1, count, sendTransactionHashes, subscribeToTransactions)

    constructor(
        bloomFilter: BloomFilter,
        fromBlockHeight: Int,
        count: Int,
        sendTransactionHashes: Boolean,
        subscribeToTransactions: SubscribeToTransactionsWithProofs
    ) :
        this(bloomFilter, Sha256Hash.ZERO_HASH, fromBlockHeight, count, sendTransactionHashes, subscribeToTransactions)

    val request: CoreOuterClass.TransactionsWithProofsRequest

    init {
        val bloomFilterBuilder = CoreOuterClass.BloomFilter.newBuilder()
            .setVData(ByteString.copyFrom(bloomFilter.data))
            .setNHashFuncs(bloomFilter.hashFuncs.toInt())
            .setNFlags(bloomFilter.flags.toInt())
            .setNTweak(bloomFilter.tweak.toInt())

        val builder = CoreOuterClass.TransactionsWithProofsRequest.newBuilder()
            .setCount(count)
            .setSendTransactionHashes(sendTransactionHashes)
            .setBloomFilter(bloomFilterBuilder.build())

        if (fromBlockHeight == -1) {
            builder.fromBlockHeight = fromBlockHeight
        } else {
            builder.fromBlockHash = ByteString.copyFrom(fromBlockHash.bytes)
        }

        request = builder.build()
    }

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.coreNonBlocking.subscribeToTransactionsWithProofs(
            request,
            object : StreamObserver<CoreOuterClass.TransactionsWithProofsResponse> {
                override fun onNext(value: CoreOuterClass.TransactionsWithProofsResponse?) {
                    val response = TransactionsWithProofsResponse(value!!)
                    subscribeToTransactions.onNext(response)
                }

                override fun onError(t: Throwable?) {
                    subscribeToTransactions.onError(t)
                }

                override fun onCompleted() {
                    subscribeToTransactions.onCompleted()
                }
            }
        )
    }

    override fun toString(): String {
        return "subscribeToTransactionsWithProofs($bloomFilter, $fromBlockHash, $fromBlockHeight, " +
            "$count, $sendTransactionHashes, ...)"
    }
}
