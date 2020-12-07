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

class GetDocumentsMethod(contractId: ByteArray, type: String, documentQuery: DocumentQuery) : GrpcMethod {

    val request: PlatformOuterClass.GetDocumentsRequest

    init {
        val builder = PlatformOuterClass.GetDocumentsRequest.newBuilder()
                .setDataContractId(ByteString.copyFrom(contractId))
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

class GetContractMethod(dataContractId: ByteArray) : GrpcMethod {

    val request = PlatformOuterClass.GetDataContractRequest.newBuilder()
            .setId(ByteString.copyFrom(dataContractId))
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getDataContract(request)
    }
}

class GetIdentityMethod(identityId: ByteArray) : GrpcMethod {

    val request = PlatformOuterClass.GetIdentityRequest.newBuilder()
            .setId(ByteString.copyFrom(identityId))
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentity(request)
    }
}

class GetIdentitiesByPublicKeyHashes(pubKeyHashes: List<ByteArray>) : GrpcMethod {

    val request = PlatformOuterClass.GetIdentitiesByPublicKeyHashesRequest.newBuilder()
                .addAllPublicKeyHashes(pubKeyHashes.map { ByteString.copyFrom(it)})
                .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentitiesByPublicKeyHashes(request) as PlatformOuterClass.GetIdentitiesByPublicKeyHashesResponse
    }
}


class GetIdentityIdsByPublicKeyHashes(pubKeyHashes: List<ByteArray>) : GrpcMethod {

    val request = PlatformOuterClass.GetIdentityIdsByPublicKeyHashesRequest.newBuilder()
            .addAllPublicKeyHashes(pubKeyHashes.map { ByteString.copyFrom(it)})
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentityIdsByPublicKeyHashes(request)
    }
}

class BroadcastStateTransitionMethod(val stateTransition: StateTransition) : GrpcMethod {

    val request = PlatformOuterClass.BroadcastStateTransitionRequest.newBuilder()
            .setStateTransition(ByteString.copyFrom(stateTransition.toBuffer()))
            .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.broadcastStateTransition(request)
    }
}