/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.provider

import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.evolution.SimplifiedMasternodeList
import org.bitcoinj.evolution.SimplifiedMasternodeListManager

class SimplifiedMasternodeListDAPIAddressProvider(
        private val smlProvider: SimplifiedMasternodeListManager,
        private val listProvider: ListDAPIAddressProvider
): DAPIAddressListProvider {

    val backupListProvider: ListDAPIAddressProvider
    private val alwaysBanAddresses = arrayListOf<String>()

    init {
        backupListProvider = ListDAPIAddressProvider(listProvider.getAllAddresses(), listProvider.baseBanTime)
    }

    override fun getLiveAddress(): DAPIAddress {
        val sml = smlProvider.listAtChainTip

        val addressesByProRegTxHash = hashMapOf<Sha256Hash, DAPIAddress>()

        listProvider.getAllAddresses().forEach {
            if (it.proRegTxHash != Sha256Hash.ZERO_HASH) {
                addressesByProRegTxHash[it.proRegTxHash] = it
            }
        }

        val updatedAddresses = arrayListOf<DAPIAddress>()

        if(sml.validMNsCount > 0) {
            sml.forEachMN(true, SimplifiedMasternodeList.ForeachMNCallback {
                var address = addressesByProRegTxHash[it.proRegTxHash]

                if (address == null) {
                    address = DAPIAddress(it.service.socketAddress.hostString, it.proRegTxHash)
                } else {
                    address.host = it.service.socketAddress.hostString
                }
                if (!alwaysBanAddresses.contains(address.host))
                    updatedAddresses.add(address)
            })

            listProvider.addresses = updatedAddresses
        }

        return if (listProvider.hasLiveAddresses())
            listProvider.getLiveAddress()
        else
            backupListProvider.getLiveAddress() // this may not be necessary, but is here in case the DML has no entries
    }

    override fun hasLiveAddresses(): Boolean {
        return listProvider.hasLiveAddresses()
    }

    override fun addBannedAddress(address: String) {
        alwaysBanAddresses.add(address)
    }

    override fun setBanBaseTime(banBaseTime: Int) {
        listProvider.setBanBaseTime(banBaseTime)
    }

    override fun getStatistics(): String {
        val currentlyBanned = listProvider.addresses.filter { it.isBanned }
        return "  ---always banned addresses: $alwaysBanAddresses\n" +
                "total masternodes          : ${listProvider.addresses.size}\n" +
                "total banned nodes         : ${currentlyBanned.size }\n" +
                "                             $currentlyBanned"
    }

    override fun getErrorStatistics(): String {
        return listProvider.getErrorStatistics()
    }
}