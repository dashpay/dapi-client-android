/**
 * Copyright (c) 2020-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.provider

interface DAPIAddressListProvider {
    fun getLiveAddress(): DAPIAddress
    fun hasLiveAddresses(): Boolean
    fun addBannedAddress(address: String)
}