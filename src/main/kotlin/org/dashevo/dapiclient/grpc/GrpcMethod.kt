/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.grpc

import org.dashevo.dapiclient.provider.DAPIGrpcMasternode

interface GrpcMethod {
    fun execute(masternode: DAPIGrpcMasternode): Any
}

