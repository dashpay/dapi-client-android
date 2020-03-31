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
        val masternodeList = EvoNetParams.MASTERNODES
        val client = OldDapiClient(SingleMasternode(masternodeList[0]).host, "3000")

        val client2 = DapiClient(EvoNetParams.MASTERNODES[0])
        println("blocking=> hash: " + client2.getBestBlockHash())
        sleep(12000)
    }
}