package org.dashj.platform.dapiclient.model

import org.dash.platform.dapi.v0.PlatformOuterClass

data class ResponseMetaData(val height: Int, val coreChainLockedHeight: Int) {
    constructor(metadata: PlatformOuterClass.ResponseMetadata) : this(metadata.height, metadata.coreChainLockedHeight)
}
