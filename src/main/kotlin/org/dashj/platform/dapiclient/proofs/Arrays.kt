/**
 * Copyright (c) 2021-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.proofs

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

/**
 * This file was translated from https://github.com/antouhou/js-merkle
 */

object Arrays {
    @JvmStatic
    fun <T, U> zip(a: List<T>, b: List<U>): List<Pair<T, U>> {
        if (a.size != b.size) {
            throw IllegalArgumentException("Cannot zip, as arrays have different lengths (${a.size} vs ${b.size}")
        }

        return a.mapIndexed { index, value -> Pair(value, b[index]) }
    }

    @JvmStatic
    fun <T> difference(a: List<T>, b: List<T>): List<T> {
        return a.filter { siblingIndex -> !b.contains(siblingIndex) }
    }

    @JvmStatic
    fun isLeftIndex(index: Int): Boolean {
        return index % 2 == 0
    }

    @JvmStatic
    fun getSiblingIndex(index: Int): Int {
        if (isLeftIndex(index)) {
            // Right sibling index
            return index + 1
        }
        // Left sibling index
        return index - 1
    }

    @JvmStatic
    fun getParentIndex(index: Int): Int {
        if (isLeftIndex(index)) {
            return index / 2
        }
        return getSiblingIndex(index) / 2
    }

    @JvmStatic
    fun getParentIndices(indices: List<Int>): List<Int> {
        // new Set removed all duplicates if two nodes were siblings
        return indices.map { getParentIndex(it) }.toSet().toList()
    }

    @JvmStatic
    fun getTreeDepth(leavesCount: Int): Int {
        return ceil(ln(leavesCount.toDouble()) / ln(2.0)).toInt()
    }

    @JvmStatic
    fun maxLeavesCountAtDepth(depth: Int): Int {
        return 2.0.pow(depth.toDouble()).toInt()
    }

    @JvmStatic
    fun getUnevenLayers(treeLeavesCount: Int): List<LayerInfo> {
        var leavesCount = treeLeavesCount
        val depth = getTreeDepth(treeLeavesCount)

        val unevenLayers = arrayListOf<LayerInfo>()

        for (index in 0 until depth) {
            val unevenLayer = leavesCount % 2 != 0
            if (unevenLayer) {
                unevenLayers.add(LayerInfo(index, leavesCount))
            }

            leavesCount = ceil(leavesCount.toDouble() / 2).toInt()
        }

        return unevenLayers
    }

    @JvmStatic
    fun getProofIndices(sortedLeafIndices: List<Int>, leavesCount: Int): List<List<Int>> {
        val depth = getTreeDepth(leavesCount)
        val unevenLayers = getUnevenLayers(leavesCount)
        val proofIndices = arrayListOf<List<Int>>()

        IntRange(0, depth - 1).fold(sortedLeafIndices) { layerNodes, layerIndex ->

            val siblingIndices = layerNodes.map { getSiblingIndex(it) }
            // Figuring out indices that are already siblings and do not require additional hash
            // to calculate the parent
            var proofNodesIndices = difference(siblingIndices, layerNodes)

            // The last node of that layer doesn't have another hash to the right, so doesn't
            val unevenLayer = unevenLayers.find { it.index == layerIndex }
            if (unevenLayer != null && layerNodes.contains(unevenLayer.leavesCount - 1)) {
                proofNodesIndices = proofNodesIndices.slice(IntRange(0, proofNodesIndices.size - 2))
            }

            proofIndices.add(proofNodesIndices)
            // Passing parent nodes indices to the next iteration cycle
            getParentIndices(layerNodes)
        }

        return proofIndices
    }

    @JvmStatic
    fun concatAndHash(
        leftNode: ByteArray?,
        rightNode: ByteArray?,
        hashFunction: (ByteArray) -> ByteArray
    ): ByteArray {
        return if (rightNode != null) {
            val concat = ByteArray(leftNode!!.size + rightNode.size)
            leftNode.copyInto(concat, 0, 0, leftNode.size)
            rightNode.copyInto(concat, leftNode.size, 0, rightNode.size)
            hashFunction(concat)
        } else {
            leftNode!!
        }
    }
}
