/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.provider

import com.google.common.base.Stopwatch
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.dash.platform.dapi.v0.CoreGrpc
import org.dash.platform.dapi.v0.PlatformGrpc
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class DAPIGrpcMasternode(address: DAPIAddress, val timeout: Long, val allowSelfSignedCertificate: Boolean) : DAPIMasternode(address) {
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
    val coreNonBlocking: CoreGrpc.CoreStub by lazy {
        CoreGrpc.newStub(channel)
    }

    // Constants
    companion object {
        private val logger = LoggerFactory.getLogger(DAPIGrpcMasternode::class.java.name)
        private const val DEFAULT_SHUTDOWN_TIMEOUT = 5L
    }

    init {
        val watch = Stopwatch.createStarted()
        if (!allowSelfSignedCertificate) {
            channel = ManagedChannelBuilder.forAddress(address.host, address.grpcPort)
                .useTransportSecurity()
                .build()
        } else {
            channel = NettyChannelBuilder.forAddress(address.host, address.grpcPort)
                .sslContext(
                    GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE) // a trust manager that trusts all certificates
                        .build()
                )
                .build()
        }

        logger.debug("Connecting to GRPC host: ${address.host}:${address.grpcPort} (time: $watch)")
    }

    fun shutdown() {
        if (!channel.isShutdown) {
            logger.debug("Shutting down: " + channel.shutdown())
            channel.shutdown().awaitTermination(DEFAULT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)
        }
    }
}
