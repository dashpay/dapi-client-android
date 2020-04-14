/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient

import kotlin.random.Random

class MultipleMasternodes(val hosts: List<String>) : MasternodeService {

    private val random = Random

    override fun getServer(): String {
        return hosts[random.nextInt(until = hosts.size)]
    }
}