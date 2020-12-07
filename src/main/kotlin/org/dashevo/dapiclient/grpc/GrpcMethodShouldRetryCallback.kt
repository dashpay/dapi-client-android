/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient.grpc

import io.grpc.StatusRuntimeException
import org.dashevo.dapiclient.DapiClient
import org.dashevo.dapiclient.model.DocumentQuery
import org.dashevo.dpp.StateRepository
import org.dashevo.dpp.contract.DataContractCreateTransition
import org.dashevo.dpp.document.DocumentCreateTransition
import org.dashevo.dpp.document.DocumentsBatchTransition
import org.dashevo.dpp.identifier.Identifier
import org.dashevo.dpp.identity.IdentityCreateTransition
import org.slf4j.LoggerFactory

/**
 * This interface contains a shouldRetry method which will be used by
 * [DapiClient.grpcRequest] to determine if a retry of the last DAPI call
 * should be attempted given the exception.
 *
 * This allows customization based on the [GrpcMethod] used.
 */
interface GrpcMethodShouldRetryCallback {
    fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean
}

/**
 * shouldRetry always returns true
 */
class DefaultShouldRetryCallback : GrpcMethodShouldRetryCallback {
    override fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean {
        return true
    }
}

/**
 * DefaultBroadcastRetryCallback will determine if a state transition was successful, but it only called
 * when [DapiClient.broadcastStateTransition] returns an error
 *
 * For [DocumentsBatchTransition]s that contain more than one document, they are all assumed to be
 * the same type as the first transition.
 *
 * @property stateRepository StateRepository Used for DAPI calls to fetch documents, identities and contracts
 * @property updatedAt Long If a document was updated in the broadcast, this will be used to identify the updated document.
 * @constructor
 */
class DefaultBroadcastRetryCallback(private val stateRepository: StateRepository,
                                    private val updatedAt: Long = -1): GrpcMethodShouldRetryCallback {
    companion object {
        private val logger = LoggerFactory.getLogger(DefaultBroadcastRetryCallback::class.java.name)
    }

    override fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean {
        logger.info("Determining if we should retry ${grpcMethod.javaClass.simpleName} ${e.status.code}")
        if (grpcMethod is BroadcastStateTransitionMethod) {
            when (grpcMethod.stateTransition) {
                is DataContractCreateTransition -> {
                    val contactCreateTransition = grpcMethod.stateTransition as DataContractCreateTransition
                    for (i in 0..5) {
                        //how to delay
                        delay()
                        val identityData = stateRepository.fetchDataContract(contactCreateTransition.dataContract.id)

                        if (identityData != null) {
                            logger.info("contract found. No need to retry: ${contactCreateTransition.dataContract.id}")
                            return false
                        }
                    }
                    logger.info("contract not found, need to retry: ${contactCreateTransition.dataContract.id}")
                }
                is IdentityCreateTransition -> {
                    val identityCreateTransition = grpcMethod.stateTransition as IdentityCreateTransition
                    for (i in 0..5) {
                        //how to delay
                        delay()
                        val identityData = stateRepository.fetchIdentity(identityCreateTransition.identityId)

                        if (identityData != null) {
                            logger.info("identity found. No need to retry: ${identityCreateTransition.identityId}")
                            return false
                        }
                    }
                    logger.info("identity not found, need to retry: ${identityCreateTransition.identityId}")
                }
                is DocumentsBatchTransition -> {
                    val documentTransitions = (grpcMethod.stateTransition as DocumentsBatchTransition).transitions

                    // this only works for document create transitions, assume the first is similar to all the
                    // rest using the same contract and document type
                    val idList = documentTransitions.map { it.id }
                    val dataContractId = documentTransitions[0].dataContractId
                    val type = documentTransitions[0].type


                    val queryBuilder = DocumentQuery.builder()
                            .where(listOf("\$id", "in", idList))

                    if (updatedAt != -1L) {
                        queryBuilder.where(listOf("updatedAt", "==", updatedAt))
                    }

                    val query = queryBuilder.build()


                    for (i in 0..5) {
                        //how to delay
                        delay()
                        val documentsData = stateRepository.fetchDocuments(dataContractId, type, query)

                        if (documentsData != null && documentsData.isNotEmpty()) {
                            logger.info("document(s) found. No need to retry: $idList")
                            return false
                        }
                    }
                    logger.info("document(s) not found, need to retry: $idList")
                }
            }
        }
        return true
    }

    private fun delay(milliseconds: Long = 3000) {
        Thread.sleep(milliseconds)
    }
}