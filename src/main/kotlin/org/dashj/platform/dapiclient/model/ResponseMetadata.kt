/**
 * Copyright (c) 2021-present, Dash Core Group
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.model

import org.dash.platform.dapi.v0.PlatformOuterClass
import org.dashj.platform.dpp.Metadata

data class ResponseMetadata(val height: Long, val coreChainLockedHeight: Int) {
    constructor(metadata: PlatformOuterClass.ResponseMetadata) : this(metadata.height, metadata.coreChainLockedHeight)

    fun getMetadata(): Metadata {
        return Metadata(height.toInt(), coreChainLockedHeight)
    }

    override fun toString(): String {
        return "ResponseMetadata(height: $height, coreHeight: $coreChainLockedHeight)"
    }
}
