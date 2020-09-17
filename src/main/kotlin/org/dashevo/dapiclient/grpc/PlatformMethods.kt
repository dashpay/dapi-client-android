/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.grpc

import com.google.protobuf.ByteString
import org.dash.platform.dapi.v0.PlatformOuterClass
import org.dashevo.dapiclient.model.DocumentQuery
import org.dashevo.dapiclient.provider.DAPIGrpcMasternode
import org.dashevo.dpp.statetransition.StateTransition

class GetDocumentsMethod(contractId: String, type: String, documentQuery: DocumentQuery) : GrpcMethod {

    val request: PlatformOuterClass.GetDocumentsRequest

    init {
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

        request = builder.build()
    }

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getDocuments(request)
    }
}

class GetContractMethod(dataContractId: String) : GrpcMethod {

    val request = PlatformOuterClass.GetDataContractRequest.newBuilder()
            .setId(dataContractId)
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getDataContract(request)
    }
}

class GetIdentityMethod(identityId: String) : GrpcMethod {

    val request = PlatformOuterClass.GetIdentityRequest.newBuilder()
            .setId(identityId)
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentity(request)
    }
}

class GetIdentityByFirstPublicKeyMethod(pubKeyHash: ByteArray) : GrpcMethod {

    val request = PlatformOuterClass.GetIdentityByFirstPublicKeyRequest.newBuilder()
            .setPublicKeyHash(ByteString.copyFrom(pubKeyHash))
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentityByFirstPublicKey(request)
    }
}

class GetIdentityIdByFirstPublicKeyMethod(pubKeyHash: ByteArray) : GrpcMethod {

    val request = PlatformOuterClass.GetIdentityIdByFirstPublicKeyRequest.newBuilder()
            .setPublicKeyHash(ByteString.copyFrom(pubKeyHash))
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentityIdByFirstPublicKey(request)
    }
}

class BroadcastStateTransitionMethod(stateTransition: StateTransition) : GrpcMethod {

    val request = PlatformOuterClass.BroadcastStateTransitionRequest.newBuilder()
            .setStateTransition(ByteString.copyFrom(stateTransition.serialize()))
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.broadcastStateTransition(request)
    }
}