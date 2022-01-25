/**
 * Copyright (c) 2020-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.model

import com.google.common.base.Preconditions
import org.dashj.platform.dpp.BaseObject
import org.dashj.platform.dpp.identifier.Identifier
import org.dashj.platform.dpp.util.Cbor
import org.json.JSONArray

/**
 * These options are used by getDocument to filter results
 * @property where MutableList<MutableList<String>>?
 * @property orderBy MutableList<MutableList<String>>?
 * @property limit Int
 * @property startAt Int
 * @property startAfter Int
 */
class DocumentQuery private constructor(
    var where: List<Any>? = null,
    var orderBy: List<List<String>>? = null,
    var limit: Int = -1,
    var startAt: Int = -1,
    var startAfter: Int = -1
) : BaseObject() {

    companion object {
        val emptyByteArray = ByteArray(0)
        const val ascending = "asc"
        const val descending = "desc"
        val operators = listOf("<", "<=", "==", ">", ">", "in", "startsWith", "elementMatch", "length", "contains")

        fun builder(): Builder { return Builder() }
    }

    data class Builder(
        var where: MutableList<List<Any>>? = null,
        var orderBy: MutableList<List<String>>? = null,
        var limit: Int = -1,
        var startAt: Int = -1,
        var startAfter: Int = -1
    ) {

        fun where(where: List<Any>) = apply {
            Preconditions.checkArgument(where.size == 3, "A where clause must have three items")
            Preconditions.checkArgument(operators.contains(where[1]), "Where clause operator must be one of : $operators")
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

        fun where(left: String, operator: String, right: ByteArray): Builder {
            return where(listOf(left, operator, right))
        }

        fun where(left: String, operator: String, right: Identifier): Builder {
            return where(listOf(left, operator, right))
        }

        fun where(left: String, operator: String, right: Int): Builder {
            return where(listOf(left, operator, right))
        }

        fun where(left: String, operator: String, right: Long): Builder {
            return where(listOf(left, operator, right))
        }

        fun whereIn(left: String, right: List<Any>): Builder {
            return where(listOf(left, "in", right))
        }

        fun orderBy(orderBy: List<String>) = apply {
            if (this.orderBy == null) {
                this.orderBy = ArrayList()
            }
            this.orderBy!!.add(orderBy)
        }

        fun orderBy(orderBy: String): Builder {
            return orderBy(JSONArray(orderBy).toList() as List<String>)
        }

        fun orderBy(index: String, orderByAscending: Boolean = true): Builder {
            return orderBy(listOf(index, if (orderByAscending) ascending else descending))
        }

        fun limit(limit: Int) = apply { this.limit = limit }
        fun startAt(startAt: Int) = apply { this.startAt = startAt }
        fun startAfter(startAfter: Int) = apply { this.startAfter = startAfter }

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
        return if (where != null) {
            Cbor.encode(where!!)
        } else {
            emptyByteArray
        }
    }

    fun encodeOrderBy(): ByteArray {
        return if (orderBy != null) {
            Cbor.encode(orderBy!!)
        } else {
            emptyByteArray
        }
    }

    override fun toObject(): Map<String, Any?> {
        val json = HashMap<String, Any?>(5)
        if (where != null) {
            json["where"] = where
        }
        if (orderBy != null) {
            json["orderBy"] = orderBy
        }
        if (limit != -1) {
            json["limit"] = limit
        }
        if (startAt != -1) {
            json["startAt"] = startAt
        }
        if (startAfter != -1) {
            json["startAfter"] = startAfter
        }
        return json
    }

    override fun toJSON(): Map<String, Any?> {
        val json = HashMap<String, Any?>(5)
        if (where != null) {
            json["where"] = where
        }
        if (orderBy != null) {
            json["orderBy"] = orderBy
        }
        if (limit != -1) {
            json["limit"] = limit
        }
        if (startAt != -1) {
            json["startAt"] = startAt
        }
        if (startAfter != -1) {
            json["startAfter"] = startAfter
        }
        return json
    }

    fun clone(): DocumentQuery {
        return DocumentQuery(where, orderBy, limit, startAt, startAfter)
    }
}
