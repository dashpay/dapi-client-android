/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.provider

import com.google.common.base.Stopwatch
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.dash.platform.dapi.v0.CoreGrpc
import org.dash.platform.dapi.v0.PlatformGrpc
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class DAPIGrpcMasternode(address: DAPIAddress, val timeout: Long) : DAPIMasternode(address) {
    // gRPC properties
    private lateinit var channel: ManagedChannel
    val platform: PlatformGrpc.PlatformBlockingStub by lazy {
        PlatformGrpc.newBlockingStub(channel).withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    }
    val platformWithoutDeadline: PlatformGrpc.PlatformBlockingStub by lazy {
        PlatformGrpc.newBlockingStub(channel)
    }
    val core: CoreGrpc.CoreBlockingStub by lazy {
        CoreGrpc.newBlockingStub(channel).withDeadlineAfter(timeout, TimeUnit.MILLISECONDS)
    }
    val coreWithoutDeadline: CoreGrpc.CoreBlockingStub by lazy {
        CoreGrpc.newBlockingStub(channel)
    }

    // Constants
    companion object {
        private val logger = LoggerFactory.getLogger(DAPIGrpcMasternode::class.java.name)
    }

    init {
        val watch = Stopwatch.createStarted()
        channel = ManagedChannelBuilder.forAddress(address.host, address.grpcPort)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build()

        logger.info("Connecting to GRPC host: ${address.host}:${address.grpcPort} (time: $watch)")
    }

    fun shutdown() {
        if (!channel.isShutdown) {
            logger.info("Shutting down: " + channel.shutdown())
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        }
    }
}