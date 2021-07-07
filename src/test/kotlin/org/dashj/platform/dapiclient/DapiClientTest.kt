/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient

import org.bitcoinj.params.TestNet3Params
import org.junit.jupiter.api.Test

class DapiClientTest {

    @Test
    fun jRPCTests() {
        val client = DapiClient(TestNet3Params.MASTERNODES.toList())
        println("blocking=> hash: " + client.getBestBlockHash())
    }

    fun getMnListDiff(): Map<String, Any> {
        val client = DapiClient(TestNet3Params.MASTERNODES.toList())
        val success = 0
        do {
            try {
                val baseBlockHash = client.getBlockHash(0)
                val blockHash = client.getBestBlockHash()
                val mnListDiff = client.getMnListDiff(baseBlockHash!!, blockHash!!) as Map<String, Any>
                return mnListDiff as Map<String, Any>
            } catch (e: Exception) {
                println("Error: $e")
            }
        } while (success == 0)
        return mapOf()
    }

    @Test
    fun getMnListDiffTest() {
        val client = DapiClient(TestNet3Params.MASTERNODES.toList())
        println("blocking=> hash: " + client.getBestBlockHash())
        println(getMnListDiff()["mnList"])
    }
}
