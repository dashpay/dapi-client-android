/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.bitcoinj.core.Sha256Hash
import org.dashj.platform.dapiclient.model.DocumentQuery
import org.dashj.platform.dapiclient.model.GrpcExceptionInfo
import org.dashj.platform.dapiclient.model.StateTransitionBroadcastException
import org.dashj.platform.dpp.StateRepository
import org.dashj.platform.dpp.contract.DataContractCreateTransition
import org.dashj.platform.dpp.document.DocumentCreateTransition
import org.dashj.platform.dpp.document.DocumentsBatchTransition
import org.dashj.platform.dpp.errors.concensus.basic.datacontract.InvalidDataContractIdException
import org.dashj.platform.dpp.errors.concensus.document.DataContractNotPresentException
import org.dashj.platform.dpp.errors.concensus.signature.IdentityNotFoundException
import org.dashj.platform.dpp.errors.concensus.state.datacontract.DataTriggerConditionException
import org.dashj.platform.dpp.errors.concensus.state.document.DocumentAlreadyPresentException
import org.dashj.platform.dpp.identifier.Identifier
import org.dashj.platform.dpp.identity.IdentityCreateTransition
import org.dashj.platform.dpp.identity.IdentityTopUpTransition
import org.dashj.platform.dpp.statetransition.StateTransition
import org.dashj.platform.dpp.toBase64
import org.dashj.platform.dpp.util.HashUtils
import org.slf4j.LoggerFactory

interface BroadcastShouldRetryCallback {
    fun shouldRetry(grpcMethod: GrpcMethod, errorInfo: StateTransitionBroadcastException): Boolean
    fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean
}

class DefaultBroadcastRetryCallback : BroadcastShouldRetryCallback {
    override fun shouldRetry(grpcMethod: GrpcMethod, errorInfo: StateTransitionBroadcastException): Boolean {
        return false
    }

    override fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean {
        return false
    }
}

/**
 * DefaultBroadcastRetryCallback will determine if a state transition was successful, but it only called
 * when [DapiClient.broadcastStateTransition] returns an error
 *
 * For [DocumentsBatchTransition]s that contain more than one document, they are all assumed to be
 * the same type as the first transition.
 *
 * Retry functionality
 *    INTERNAL, DEADLINE exceptions: check to see if the document, identity or contract exists
 *    INVALID_ARGUMENT: Invalid contract or identity. Check if the contract or identity is part of the
 *      retry Id lists which should be verified through other calls to getDocuments, getIdentity, getContract
 *
 * @property stateRepository StateRepository Used for DAPI calls to fetch documents, identities and contracts
 * @property updatedAt Long If a document was updated in the broadcast, this will be used to identify the updated document.
 * @constructor
 */
open class BroadcastRetryCallback(
    private val stateRepository: StateRepository,
    private val updatedAt: Long = -1,
    private val retryCount: Int = DEFAULT_RETRY_COUNT,
    protected open val retryContractIds: List<Identifier> = listOf(),
    protected open val retryIdentityIds: List<Identifier> = listOf(),
    protected open val retryDocumentIds: List<Identifier> = listOf(),
    protected open val retryPreorderSalts: Map<Sha256Hash, Sha256Hash> = hashMapOf()
) : BroadcastShouldRetryCallback {
    companion object {
        private val logger = LoggerFactory.getLogger(BroadcastRetryCallback::class.java.name)
        const val DEFAULT_RETRY_COUNT = 5
    }

    override fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean {
        logger.info("Determining if we should retry ${grpcMethod.javaClass.simpleName} ${e.status.code}")
        if (e.status.code == Status.UNAUTHENTICATED.code) {
            return false
        }
        if (grpcMethod is BroadcastStateTransitionMethod) {
            if (e.status.code == Status.INVALID_ARGUMENT.code) {
                logger.info("--> INVALID_ARGUMENT")
                // only retry if it is DocumentsBatchTransition
                // throw exception for any other invalid argument errors
                when (GrpcExceptionInfo(e).exception) {
                    is IdentityNotFoundException -> {
                        if (shouldRetryIdentityNotFound(grpcMethod.stateTransition)) {
                            logger.info("---retry based on IdentityNotFoundError")
                            return true
                        } else {
                            logger.info("---will not retry based on IdentityNotFoundError")
                        }
                    }
                    is DataTriggerConditionException -> {
                        // TODO: check for "preorderDocument was not found"?
                        if (shouldRetryPreorderNotFound(grpcMethod.stateTransition as DocumentsBatchTransition)) {
                            return true
                        }
                    }
                }

                // there is another case that needs to be handled below for DocumentsBatchTransition
                if (grpcMethod.stateTransition !is DocumentsBatchTransition) {
                    throw e
                }
            }

            when (grpcMethod.stateTransition) {
                is DataContractCreateTransition -> {
                    val contactCreateTransition = grpcMethod.stateTransition as DataContractCreateTransition
                    for (i in 0 until retryCount) {
                        // how to delay
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
                    for (i in 0 until retryCount) {
                        // how to delay
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
                    val documentTransitions = grpcMethod.stateTransition.transitions

                    // this only works for document create transitions, assume the first is similar to all the
                    // rest using the same contract and document type
                    val idList = documentTransitions.map { it.id }
                    val dataContractId = documentTransitions[0].dataContractId
                    val type = documentTransitions[0].type

                    if (e.status.code == Status.INVALID_ARGUMENT.code) {
                        val exception = GrpcExceptionInfo(e).exception
                        when (exception) {
                            is InvalidDataContractIdException -> {
                                if (retryContractIds.contains(dataContractId)) {
                                    logger.info("Retry ${grpcMethod.javaClass.simpleName} ${e.status.code} since error was InvalidContractIdError")
                                    return true
                                }
                            }
                            is DataContractNotPresentException -> {
                                if (retryContractIds.contains(dataContractId)) {
                                    logger.info("Retry ${grpcMethod.javaClass.simpleName} ${e.status.code} since error was DataContractNotPresentError")
                                    return true
                                }
                            }
                            is DocumentAlreadyPresentException -> {
                                if (retryContractIds.contains(dataContractId)) {
                                    logger.info("Retry ${grpcMethod.javaClass.simpleName} ${e.status.code} since error was DuplicateDocumentError")
                                    return false
                                }
                            }
                            else -> {
                                // throw exception for any other invalid argument errors
                                throw e
                            }
                        }

                        val queryBuilder = DocumentQuery.builder()
                            .where(listOf("\$id", "in", idList))

                        if (updatedAt != -1L) {
                            queryBuilder.where(listOf("updatedAt", "==", updatedAt))
                        }

                        val query = queryBuilder.build()

                        for (i in 0 until retryCount) {
                            // how to delay
                            delay()
                            val documentsData = stateRepository.fetchDocuments(dataContractId, type, query)

                            if (documentsData.isNotEmpty()) {
                                logger.info("document(s) found. No need to retry: $idList")
                                return false
                            }
                        }
                        logger.info("document(s) not found, need to retry: $idList")
                    }
                }
            }
        }
        return true
    }

    override fun shouldRetry(grpcMethod: GrpcMethod, errorInfo: StateTransitionBroadcastException): Boolean {
        logger.info("Determining if we should retry ${grpcMethod.javaClass.simpleName} ${errorInfo.errorMessage}")
        if (grpcMethod is BroadcastStateTransitionMethod) {
            logger.info("-->${errorInfo.code} was the invalid argument type from waitForSTResult")

            when (errorInfo.exception) {
                // TODO: not sure how to handle these errors
                // if multiple nodes return these errors, we have a bigger problem
                is IdentityNotFoundException -> {
                    if (shouldRetryIdentityNotFound(grpcMethod.stateTransition)) {
                        logger.info("---retry based on IdentityNotFoundError")
                        return true
                    } else {
                        logger.info("---will not retry based on IdentityNotFoundError")
                    }
                }
                is DataTriggerConditionException -> {
                    // TODO: check for "preorderDocument was not found")
                    if (shouldRetryPreorderNotFound(grpcMethod.stateTransition as DocumentsBatchTransition)) {
                        return true
                    }
                }
            }

            // there is another case that needs to be handled below for DocumentsBatchTransition
            if (grpcMethod.stateTransition !is DocumentsBatchTransition) {
                throw errorInfo
            }

            when (grpcMethod.stateTransition) {
                is DataContractCreateTransition -> {
                    for (i in 0 until retryCount) {
                        // how to delay
                        delay()
                        val identityData =
                            stateRepository.fetchDataContract(grpcMethod.stateTransition.dataContract.id)

                        if (identityData != null) {
                            logger.info("contract found. No need to retry: ${grpcMethod.stateTransition.dataContract.id}")
                            return false
                        }
                    }
                    logger.info("contract not found, need to retry: ${grpcMethod.stateTransition.dataContract.id}")
                }
                is IdentityCreateTransition -> {
                    val identityCreateTransition = grpcMethod.stateTransition as IdentityCreateTransition
                    for (i in 0 until retryCount) {
                        // how to delay
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
                    val documentTransitions = grpcMethod.stateTransition.transitions

                    // this only works for document create transitions, assume the first is similar to all the
                    // rest using the same contract and document type
                    val idList = documentTransitions.map { it.id }
                    val dataContractId = documentTransitions[0].dataContractId
                    val type = documentTransitions[0].type

                    when (errorInfo.exception) {
                        is InvalidDataContractIdException -> {
                            if (retryContractIds.contains(Identifier.from(dataContractId))) {
                                logger.info("Retry ${grpcMethod.javaClass.simpleName} ${errorInfo.errorMessage} since error was InvalidContractIdError")
                                return true
                            }
                        }
                        is DataContractNotPresentException -> {
                            if (retryContractIds.contains(Identifier.from(dataContractId))) {
                                logger.info("Retry ${grpcMethod.javaClass.simpleName} ${errorInfo.errorMessage} since error was DataContractNotPresentError")
                                return true
                            }
                        }
                        is DocumentAlreadyPresentException -> {
                            if (retryContractIds.contains(Identifier.from(dataContractId))) {
                                logger.info("Not retrying ${grpcMethod.javaClass.simpleName} ${errorInfo.errorMessage} since error was DuplicateDocumentError")
                                return false
                            }
                        }
                        // throw exception for any other invalid argument errors
                        else -> throw errorInfo
                    }

                    val queryBuilder = DocumentQuery.builder()
                        .where(listOf("\$id", "in", idList))

                    if (updatedAt != -1L) {
                        queryBuilder.where(listOf("updatedAt", "==", updatedAt))
                    }

                    val query = queryBuilder.build()

                    for (i in 0 until retryCount) {
                        // how to delay
                        delay()
                        val documentsData = stateRepository.fetchDocuments(dataContractId, type, query)

                        if (documentsData.isNotEmpty()) {
                            logger.info("document(s) found. No need to retry: $idList")
                            return false
                        }
                    }
                    logger.info("document(s) not found, need to retry: $idList")
                } else -> {
                    // do nothing in the case of other types of transitions
                }
            }
        }
        return true
    }

    private fun shouldRetryIdentityNotFound(stateTransition: StateTransition): Boolean {
        return when (stateTransition) {
            is DocumentsBatchTransition -> {
                logger.info("---looking for ${stateTransition.ownerId} in $retryIdentityIds")
                retryIdentityIds.contains(stateTransition.ownerId)
            }
            is DataContractCreateTransition -> {
                logger.info("---looking for ${stateTransition.dataContract.ownerId} in $retryIdentityIds")
                retryIdentityIds.contains(stateTransition.dataContract.ownerId)
            }
            is IdentityTopUpTransition -> {
                logger.info("---looking for ${stateTransition.identityId} in $retryIdentityIds")
                retryIdentityIds.contains(stateTransition.identityId)
            }
            else -> false
        }
    }
    // TODO: This is not all that useful and is currently unused
    private fun shouldRetryDocumentNotFound(stateTransition: StateTransition): Boolean {
        if (stateTransition is DocumentsBatchTransition) {
            logger.info("---looking for ${stateTransition.transitions[0].id} in $retryDocumentIds")
            if (retryDocumentIds.contains(stateTransition.transitions[0].id)) {
                return true
            }
        }
        return false
    }

    private fun shouldRetryPreorderNotFound(stateTransition: DocumentsBatchTransition): Boolean {
        if (stateTransition.transitions[0] is DocumentCreateTransition) {
            val createTransition = stateTransition.transitions[0] as DocumentCreateTransition
            val preorderSalt = HashUtils.byteArrayFromBase64orByteArray(createTransition.data["preorderSalt"]!!)
            logger.info("---looking for ${preorderSalt.toBase64()}")
            if (retryPreorderSalts.containsKey(preorderSalt)) {
                for (i in 0 until retryCount) {
                    // how to delay
                    delay()
                    val documentsData = stateRepository.fetchDocuments(
                        createTransition.dataContractId,
                        "preorder",
                        DocumentQuery.builder()
                            .where("saltedDomainHash", "==", retryPreorderSalts[preorderSalt]!!.bytes)
                    )

                    if (documentsData.isNotEmpty()) {
                        logger.info("document(s) found. No need to retry: ${preorderSalt.toBase64()}")
                        return false
                    }
                }
                logger.info("document(s) not found, need to retry: ${preorderSalt.toBase64()}")
            }
        }
        return true
    }

    // override fun shouldThrowException(e: StatusRuntimeException): Boolean {
    //    return super.shouldThrowException(e) && e.status.code != Status.INVALID_ARGUMENT.code
    // }

    private fun delay(milliseconds: Long = 3000) {
        Thread.sleep(milliseconds)
    }
}
