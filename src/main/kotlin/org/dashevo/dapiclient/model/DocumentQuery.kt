/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.model

import org.dashevo.dpp.util.Cbor

/**
 * These options are used by getDocument to filter results
 * @property where MutableList<MutableList<String>>?
 * @property orderBy MutableList<MutableList<String>>?
 * @property limit Int
 * @property startAt Int
 * @property startAfter Int
 */
class DocumentQuery {

    companion object {
        val emptyByteArray = ByteArray(0)
    }
    var where: MutableList<MutableList<String>>? = null
    var orderBy: MutableList<MutableList<String>>? = null
    var limit: Int = 100
    var startAt: Int = 0
    var startAfter: Int = 0

    constructor(where: MutableList<MutableList<String>>?,
                orderBy: MutableList<MutableList<String>>?,
                limit: Int,
                startAt: Int,
                startAfter: Int) {
        this.where = where
        this.orderBy = orderBy
        this.limit = limit
        this.startAfter = startAfter
        this.startAt = startAt
    }

    fun encodeWhere(): ByteArray {
        return if(where != null) {
            Cbor.encode(where!!)
        } else emptyByteArray
    }

    fun encodeOrderBy(): ByteArray {
        return if(orderBy != null) {
            Cbor.encode(orderBy!!)
        } else emptyByteArray
    }

    //TODO: Add methods to clear and add more filters to where and orderBy
}