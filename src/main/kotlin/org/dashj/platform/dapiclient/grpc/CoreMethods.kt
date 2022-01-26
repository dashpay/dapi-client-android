/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.grpc

import com.google.protobuf.ByteString
import org.dash.platform.dapi.v0.CoreOuterClass
import org.dashj.platform.dapiclient.provider.DAPIGrpcMasternode

class GetBlockMethod(private val getBlockRequest: CoreOuterClass.GetBlockRequest) : GrpcMethod {
    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.getBlock(getBlockRequest)
    }

    override fun toString(): String {
        return "getBlock(" + if (getBlockRequest.hash != null) {
            getBlockRequest.hash
        } else {
            getBlockRequest.height
        }
    }
}

class GetStatusMethod : GrpcMethod {
    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.getStatus(CoreOuterClass.GetStatusRequest.newBuilder().build())
    }

    override fun toString(): String {
        return "getStatus"
    }
}

class BroadcastTransactionMethod(txBytes: ByteArray, allowHighFees: Boolean, bypassLimits: Boolean) : GrpcMethod {

    val request = CoreOuterClass.BroadcastTransactionRequest.newBuilder()
        .setTransaction(ByteString.copyFrom(txBytes))
        .setAllowHighFees(allowHighFees)
        .setBypassLimits(bypassLimits)
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.broadcastTransaction(request)
    }
}

class GetTransactionMethod(private val txHex: String) : GrpcMethod {
    val request = CoreOuterClass.GetTransactionRequest.newBuilder()
        .setId(txHex)
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.getTransaction(request)
    }

    override fun toString(): String {
        return "getTransaction($txHex)"
    }
}

class GetEstimatedTransactionFeeMethod(private val blocks: Int) : GrpcMethod {
    val request = CoreOuterClass.GetEstimatedTransactionFeeRequest.newBuilder()
        .setBlocks(blocks)
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.getEstimatedTransactionFee(request)
    }

    override fun toString(): String {
        return "getEstimatedTransactionFeeMethod($blocks)"
    }
}
