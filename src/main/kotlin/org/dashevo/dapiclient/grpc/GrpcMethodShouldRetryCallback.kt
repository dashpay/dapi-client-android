/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashevo.dapiclient.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.dashevo.dapiclient.DapiClient
import org.dashevo.dapiclient.model.GrpcExceptionInfo
import org.dashevo.dpp.identifier.Identifier
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
    fun shouldThrowException(e: StatusRuntimeException): Boolean
}

/**
 * shouldRetry always returns true
 */
open class DefaultShouldRetryCallback : GrpcMethodShouldRetryCallback {
    override fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean {
        return when (e.status.code) {
            Status.INVALID_ARGUMENT.code  -> {
                // do not retry any invalid argument errors
                false
            }
            Status.NOT_FOUND.code -> {
                // do not retry any not found errors
                false
            }
            else -> true
        }
    }

    override fun shouldThrowException(e: StatusRuntimeException): Boolean {
        return e.status.code != Status.DEADLINE_EXCEEDED.code
                && e.status.code != Status.UNAVAILABLE.code
                && e.status.code != Status.INTERNAL.code
                && e.status.code != Status.CANCELLED.code
                && e.status.code != Status.UNKNOWN.code
    }
}

open class DefaultGetDocumentsRetryCallback() : DefaultShouldRetryCallback()

open class DefaultGetIdentityRetryCallback : DefaultShouldRetryCallback()

open class DefaultGetDocumentsWithContractIdRetryCallback(protected open val retryContractIds: List<Identifier>) :
    DefaultShouldRetryCallback() {
    companion object {
        private val logger = LoggerFactory.getLogger(DefaultGetDocumentsWithContractIdRetryCallback::class.java.name)
    }

    override fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean {
        grpcMethod as GetDocumentsMethod
        if (e.status.code == Status.INVALID_ARGUMENT.code) {
            val error = GrpcExceptionInfo(e).errors[0]
            if (error.containsKey("name") && error["name"] == "InvalidContractIdError") {
                if (retryContractIds.contains(Identifier.from(grpcMethod.request.dataContractId.toByteArray()))) {
                    logger.info("Retry ${grpcMethod.javaClass.simpleName} ${e.status.code} since error was InvalidContractIdError")
                    return true
                }
            }
            // throw exception for any other invalid argument errors
            throw e
        }
        return super.shouldRetry(grpcMethod, e)
    }

    override fun shouldThrowException(e: StatusRuntimeException): Boolean {
        return super.shouldThrowException(e) && e.status.code != Status.INVALID_ARGUMENT.code
    }
}

open class DefaultGetIdentityWithIdentitiesRetryCallback(protected open val retryIdentityIds: List<Identifier> = listOf()) :
    DefaultShouldRetryCallback() {
    companion object {
        private val logger = LoggerFactory.getLogger(DefaultGetIdentityWithIdentitiesRetryCallback::class.java.name)
    }

    override fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean {
        grpcMethod as GetIdentityMethod
        if (e.status.code == Status.NOT_FOUND.code) {
            if (retryIdentityIds.contains(Identifier.from(grpcMethod.request.id.toByteArray()))) {
                logger.info("Retry $grpcMethod): ${e.status.code} since error was NOT_FOUND")
                return true
            }
        }
        return super.shouldRetry(grpcMethod, e)
    }

    override fun shouldThrowException(e: StatusRuntimeException): Boolean {
        return super.shouldThrowException(e) && e.status.code != Status.INVALID_ARGUMENT.code
    }
}


open class DefaultGetContractRetryCallback : DefaultShouldRetryCallback()

open class DefaultGetDataContractWithContractIdRetryCallback(protected open val retryContractIds: List<Identifier> = listOf()) :
    DefaultShouldRetryCallback() {
    companion object {
        private val logger = LoggerFactory.getLogger(DefaultGetDataContractWithContractIdRetryCallback::class.java.name)
    }

    override fun shouldRetry(grpcMethod: GrpcMethod, e: StatusRuntimeException): Boolean {
        grpcMethod as GetContractMethod
        if (e.status.code == Status.NOT_FOUND.code) {
            if (retryContractIds.contains(Identifier.from(grpcMethod.request.id.toByteArray()))) {
                logger.info("Retry $grpcMethod: ${e.status.code} since error was contract not found")
                return true
            }

            // throw exception for any other invalid argument errors
            throw e
        }
        return super.shouldRetry(grpcMethod, e)
    }

    override fun shouldThrowException(e: StatusRuntimeException): Boolean {
        return super.shouldThrowException(e) && e.status.code != Status.INVALID_ARGUMENT.code
    }
}

