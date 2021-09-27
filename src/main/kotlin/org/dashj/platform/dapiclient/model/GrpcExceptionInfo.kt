package org.dashj.platform.dapiclient.model

import io.grpc.StatusRuntimeException
import org.dashj.platform.dpp.errors.ErrorMetadata
import org.dashj.platform.dpp.errors.concensus.ConcensusException

class GrpcExceptionInfo(trailers: String) {
    val exception: ConcensusException

    constructor(statusRuntimeException: StatusRuntimeException) : this(statusRuntimeException.trailers.toString())

    init {
        val metadata = ErrorMetadata(trailers)
        exception = ConcensusException.create(metadata.code, metadata.arguments)
    }

    override fun toString(): String {
        return exception.toString()
    }
}
