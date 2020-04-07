/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashevo.dapiclient.model

import org.dashevo.dpp.BaseObject
import org.dashevo.dpp.util.Cbor
import org.json.JSONArray

/**
 * These options are used by getDocument to filter results
 * @property where MutableList<MutableList<String>>?
 * @property orderBy MutableList<MutableList<String>>?
 * @property limit Int
 * @property startAt Int
 * @property startAfter Int
 */
class DocumentQuery private constructor(var where: List<List<String>>? = null,
                                        var orderBy: List<List<String>>? = null,
                                        var limit: Int = -1,
                                        var startAt: Int = -1,
                                        var startAfter: Int = -1) : BaseObject() {

    companion object {
        val emptyByteArray = ByteArray(0)
    }

    data class Builder(var where: MutableList<List<String>>? = null,
                       var orderBy: MutableList<List<String>>? = null,
                       var limit: Int = -1,
                       var startAt: Int = -1,
                       var startAfter: Int = -1) {

        fun where(where: List<String>) = apply {
            if (this.where == null) {
                this.where = ArrayList()
            }
            this.where!!.add(where)
        }

        fun where(where: String): Builder {
            return where(JSONArray(where) as MutableList<String>)
        }

        fun where(left: String, operator: String, right: String): Builder {
            return where(listOf(left, operator, right))
        }

        fun orderBy(orderBy: MutableList<String>)  = apply {
            if (this.orderBy == null) {
                this.orderBy = ArrayList()
            }
            this.orderBy!!.add(orderBy)
        }

        fun orderBy(orderBy: List<String>) {
            orderBy(orderBy.toMutableList())
        }

        fun orderBy(orderBy: String) {
            orderBy(JSONArray(orderBy).toMutableList() as MutableList<String>)
        }

        fun limit(limit: Int) = apply { this.limit = limit }
        fun startAt(startAt: Int) = apply { this.startAt = startAt }
        fun startAfter(startAfter: Int) = apply {this.startAfter = startAfter}

        fun build() = DocumentQuery(where, orderBy, limit, startAt, startAfter)
    }

    fun hasLimit(): Boolean {
        return limit != -1
    }

    fun hasStartAt(): Boolean {
        return startAt != -1
    }

    fun hasStartAfter(): Boolean {
        return startAfter != -1
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

    override fun toJSON(): Map<String, Any?> {
        val json = HashMap<String, Any?>(5)
        if(where != null)
            json["where"] = where
        if(orderBy != null)
            json["orderBy"] = orderBy
        if(limit != -1)
            json["limit"] = limit
        if(startAt != -1)
            json["startAt"] = startAt
        if(startAfter != -1)
            json["startAfter"] = startAfter
        return json
    }
}