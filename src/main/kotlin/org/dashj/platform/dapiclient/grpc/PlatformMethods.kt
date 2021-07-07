/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.grpc

import com.google.protobuf.ByteString
import org.bitcoinj.core.Sha256Hash
import org.dash.platform.dapi.v0.PlatformOuterClass
import org.dashj.platform.dapiclient.model.DocumentQuery
import org.dashj.platform.dapiclient.provider.DAPIGrpcMasternode
import org.dashj.platform.dpp.statetransition.StateTransition
import org.dashj.platform.dpp.toBase58

class GetDocumentsMethod(private val contractId: ByteArray, private val type: String, private val documentQuery: DocumentQuery) : GrpcMethod {

    val request: PlatformOuterClass.GetDocumentsRequest

    init {
        val builder = PlatformOuterClass.GetDocumentsRequest.newBuilder()
            .setDataContractId(ByteString.copyFrom(contractId))
            .setDocumentType(type)
            .setWhere(ByteString.copyFrom(documentQuery.encodeWhere()))
            .setOrderBy(ByteString.copyFrom(documentQuery.encodeOrderBy()))
        if (documentQuery.hasLimit()) {
            builder.limit = documentQuery.limit
        }
        if (documentQuery.hasStartAfter()) {
            builder.startAfter = documentQuery.startAfter
        }
        if (documentQuery.hasStartAt()) {
            builder.startAt = documentQuery.startAt
        }

        request = builder.build()
    }

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getDocuments(request)
    }

    override fun toString(): String {
        return "getDocument(${contractId.toBase58()}, $type, ${documentQuery.toJSON()})"
    }
}

class GetContractMethod(val dataContractId: ByteArray) : GrpcMethod {

    val request: PlatformOuterClass.GetDataContractRequest = PlatformOuterClass.GetDataContractRequest.newBuilder()
        .setId(ByteString.copyFrom(dataContractId))
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getDataContract(request)
    }

    override fun toString(): String {
        return "getContract(${dataContractId.toBase58()})"
    }
}

class GetIdentityMethod(private val identityId: ByteArray) : GrpcMethod {

    val request: PlatformOuterClass.GetIdentityRequest = PlatformOuterClass.GetIdentityRequest.newBuilder()
        .setId(ByteString.copyFrom(identityId))
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentity(request)
    }

    override fun toString(): String {
        return "getIdentity(${identityId.toBase58()})"
    }
}

class GetIdentitiesByPublicKeyHashes(private val pubKeyHashes: List<ByteArray>) : GrpcMethod {

    val request: PlatformOuterClass.GetIdentitiesByPublicKeyHashesRequest = PlatformOuterClass.GetIdentitiesByPublicKeyHashesRequest.newBuilder()
        .addAllPublicKeyHashes(pubKeyHashes.map { ByteString.copyFrom(it) })
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentitiesByPublicKeyHashes(request) as PlatformOuterClass.GetIdentitiesByPublicKeyHashesResponse
    }

    override fun toString(): String {
        return "getIdentitiesByPublicKeyHashes($pubKeyHashes)"
    }
}

class GetIdentityIdsByPublicKeyHashes(private val pubKeyHashes: List<ByteArray>) : GrpcMethod {

    val request: PlatformOuterClass.GetIdentityIdsByPublicKeyHashesRequest = PlatformOuterClass.GetIdentityIdsByPublicKeyHashesRequest.newBuilder()
        .addAllPublicKeyHashes(pubKeyHashes.map { ByteString.copyFrom(it) })
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platform.getIdentityIdsByPublicKeyHashes(request)
    }

    override fun toString(): String {
        return "getIdentityIdsByPublicKeyHashes($pubKeyHashes)"
    }
}

class BroadcastStateTransitionMethod(val stateTransition: StateTransition) : GrpcMethod {

    val request: PlatformOuterClass.BroadcastStateTransitionRequest = PlatformOuterClass.BroadcastStateTransitionRequest.newBuilder()
        .setStateTransition(ByteString.copyFrom(stateTransition.toBuffer()))
        .build()

    override fun execute(masternode: DAPIGrpcMasternode): Any {
        return masternode.platformWithoutDeadline.broadcastStateTransition(request)
    }

    override fun toString(): String {
        return "broadcastStateTransition(${stateTransition.toJSON()}): sha256: ${Sha256Hash.of(stateTransition.toBuffer())}"
    }
}
