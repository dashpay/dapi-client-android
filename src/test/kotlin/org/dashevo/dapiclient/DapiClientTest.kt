/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient

import org.bitcoinj.params.EvoNetParams
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep

class DapiClientTest {

    @Test
    fun jRPCTests() {
        val client = DapiClient(EvoNetParams.MASTERNODES[0])
        println("blocking=> hash: " + client.getBestBlockHash())
    }
}