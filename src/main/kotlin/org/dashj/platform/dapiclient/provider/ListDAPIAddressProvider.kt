/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.provider

import java.util.Date
import java.util.Random
import kotlin.math.exp
import kotlin.math.floor

class ListDAPIAddressProvider(var addresses: List<DAPIAddress>, var baseBanTime: Int) : DAPIAddressListProvider {
    private val random = Random()
    companion object {
        fun fromList(addresses: List<String>, baseBanTime: Int): ListDAPIAddressProvider {
            return ListDAPIAddressProvider(addresses.map { DAPIAddress(it) }, baseBanTime)
        }
    }
    private val alwaysBanAddresses = arrayListOf<String>()

    override fun getLiveAddress(): DAPIAddress {
        val liveAddresses = getLiveAddresses()

        return sample(liveAddresses)
    }

    override fun hasLiveAddresses(): Boolean {
        return getLiveAddresses().isNotEmpty()
    }

    fun getAllAddresses(): List<DAPIAddress> {
        return addresses
    }

    fun getLiveAddresses(): List<DAPIAddress> {
        val now = Date().time

        return addresses.filter {
            when {
                alwaysBanAddresses.contains(it.host) -> false

                !it.isBanned -> true

                else -> {
                    val coefficient: Double = exp(it.banCount.toDouble() - 1)
                    val banPeriod = floor(coefficient) * baseBanTime

                    now > it.banStartTime + banPeriod
                }
            }
        }
    }

    private fun sample(addresses: List<DAPIAddress>): DAPIAddress {
        if (addresses.isEmpty()) {
            throw IllegalStateException("There are no live addresses from which to get a node")
        }
        return addresses[random.nextInt(addresses.size)]
    }

    override fun addBannedAddress(address: String) {
        alwaysBanAddresses.add(address)
    }

    override fun setBanBaseTime(banBaseTime: Int) {
        this.baseBanTime = banBaseTime
    }

    override fun getStatistics(): String {
        val currentlyBanned = addresses.filter { it.isBanned }
        val sb = StringBuffer()
        currentlyBanned.forEach { sb.append(it.exception) }
        return "  ---always banned addresses: $alwaysBanAddresses\n" +
            "total masternodes          : ${addresses.size}\n" +
            "total banned nodes         : ${currentlyBanned.size }\n" +
            "                             $currentlyBanned\n" +
            "$sb"
    }

    override fun getErrorStatistics(): String {
        return addresses.filter { it.isBanned }.map { it.exception }.toString()
    }
}
