/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.grpc

import com.google.protobuf.ByteString
import org.dash.platform.dapi.v0.PlatformOuterClass
import org.dashj.platform.dapiclient.provider.DAPIGrpcMasternode
import org.dashj.platform.dpp.toHexString

class WaitForStateTransitionResultMethod(val hash: ByteArray, val prove: Boolean) : GrpcMethod {

    val request: PlatformOuterClass.WaitForStateTransitionResultRequest = PlatformOuterClass.WaitForStateTransitionResultRequest.newBuilder()
        .setStateTransitionHash(ByteString.copyFrom(hash))
        .setProve(prove)
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platformWithoutDeadline.waitForStateTransitionResult(request)
    }

    override fun toString(): String {
        return "waitForStateTransitionResult(hash=${hash.toHexString()}, prove=$prove)"
    }
}
