/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.grpc

import com.google.protobuf.ByteString
import org.dash.platform.dapi.v0.CoreOuterClass
import org.dashevo.dapiclient.provider.DAPIGrpcMasternode

class GetBlockMethod(private val getBlockRequest: CoreOuterClass.GetBlockRequest) : GrpcMethod {
    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.getBlock(getBlockRequest)
    }
}

class GetStatusMethod : GrpcMethod {
    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.getStatus(CoreOuterClass.GetStatusRequest.newBuilder().build())
    }
}

class SendTransactionMethod(txBytes: ByteString, allowHighFees: Boolean, bypassLimits: Boolean) : GrpcMethod {

    val request = CoreOuterClass.SendTransactionRequest.newBuilder()
            .setTransaction(txBytes)
            .setAllowHighFees(allowHighFees)
            .setBypassLimits(bypassLimits)
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.sendTransaction(request)
    }
}

class GetTransactionMethod(txHex: String) : GrpcMethod {
    val request = CoreOuterClass.GetTransactionRequest.newBuilder()
            .setId(txHex)
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.getTransaction(request)
    }
}

class GetEstimatedTransactionFeeMethod(blocks: Int) : GrpcMethod {
    val request = CoreOuterClass.GetEstimatedTransactionFeeRequest.newBuilder()
            .setBlocks(blocks)
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.core.getEstimatedTransactionFee(request)
    }
}