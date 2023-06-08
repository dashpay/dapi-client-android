package org.dashj.platform.dapiclient

import org.bitcoinj.utils.BriefLogFormatter

open class BaseTest {
    companion object {
        init {
            BriefLogFormatter.init()
        }
    }
}