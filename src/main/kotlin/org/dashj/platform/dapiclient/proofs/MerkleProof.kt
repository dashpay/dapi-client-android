/**
 * Copyright (c) 2021-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.proofs

import org.dashj.platform.dapiclient.proofs.Arrays.concatAndHash
import org.dashj.platform.dapiclient.proofs.Arrays.getParentIndices
import org.dashj.platform.dapiclient.proofs.Arrays.getProofIndices
import org.dashj.platform.dapiclient.proofs.Arrays.zip
import org.dashj.platform.dpp.toHex

/**
 * This class was translated from https://github.com/antouhou/js-merkle
 */

data class LayerInfo(val index: Int, val leavesCount: Int)

class MerkleProof(val proofHashes: List<ByteArray>, val hashFunction: (ByteArray) -> ByteArray) {

    companion object {
        private const val HASH_SIZE = 32

        @JvmStatic
        fun fromBuffer(rootTreeProof: ByteArray, hashFunction: (ByteArray) -> ByteArray): MerkleProof {
            val length = rootTreeProof.size / HASH_SIZE
            var offset = 0
            val proofHashes = arrayListOf<ByteArray>()
            for (i in 0 until length) {
                proofHashes.add(rootTreeProof.copyOfRange(offset, offset + HASH_SIZE))
                offset += HASH_SIZE
            }
            return MerkleProof(proofHashes, hashFunction)
        }

        fun fromBytes(bytes: ByteArray, hashFunction: (ByteArray) -> ByteArray): MerkleProof {
            return fromBuffer(bytes, hashFunction)
        }
    }

    fun getHexProofHashes(): List<String> {
        return proofHashes.map { it.toHex() }
    }

    fun calculateRoot(
        leafIndices: List<Int>,
        leafHashes: List<ByteArray>,
        totalLeavesCount: Int
    ): ByteArray {
        val leafTuples = zip(leafIndices, leafHashes).sortedWith { p0, p1 -> p0!!.first - p1!!.first }
        val proofIndices = getProofIndices(leafTuples.map { it.first }, totalLeavesCount)

        var nextSliceStart = 0
        val proofTuplesByLayers = proofIndices.map { indices ->
            val sliceStart = nextSliceStart
            nextSliceStart += indices.size
            zip(indices, proofHashes.slice(IntRange(sliceStart, nextSliceStart - 1)))
        }

        val tree = arrayListOf(leafTuples)

        for (layerIndex in proofTuplesByLayers.indices) {
            // Sorted by their position in the tree, so we can take pairs correctly

            val currentLayerList = arrayListOf<Pair<Int, ByteArray>>()
            currentLayerList.addAll(proofTuplesByLayers[layerIndex])
            currentLayerList.addAll(tree[layerIndex])
            val currentLayer = currentLayerList.sortedWith { p0, p1 -> p0!!.first - p1!!.first }
                .map { it.second }

            val parentIndices = getParentIndices(tree[layerIndex].map { it.first })
            val parentLayer = parentIndices.mapIndexed { i, parentNodeTreeIndex ->
                val rightNode = if (currentLayer.size > (i * 2 + 1)) {
                    currentLayer[i * 2 + 1]
                } else {
                    null
                }
                Pair(
                    parentNodeTreeIndex,
                    concatAndHash(currentLayer[i * 2], rightNode, hashFunction)
                )
            }

            tree.add(parentLayer)
        }

        return tree[tree.size - 1][0].second
    }

    /**
     * Verifies the proof for a given root and leaves
     *
     * @param root - expected root
     * @param {number[]} leafIndices - positions of the leaves in the original tree
     * @param {ByteArray[]} leafHashes - leaf hashes to verify
     * @param {number} leavesCount - amount of leaves in the original tree
     *
     * @return boolean
     */
    fun verify(root: ByteArray, leafIndices: List<Int>, leafHashes: List<ByteArray>, leavesCount: Int): Boolean {
        val extractedRoot = calculateRoot(leafIndices, leafHashes, leavesCount)
        val rootHaveSameLength = root.size == extractedRoot.size
        var mismatch = false
        return if (rootHaveSameLength) {
            extractedRoot.forEachIndexed { index, byte ->
                if (byte != root[index]) { mismatch = true }
            }
            mismatch.not()
        } else {
            false
        }
    }

    /**
     * Serializes proof to ByteArray
     *
     * @return ByteArray
     */
    fun toBytes(): ByteArray {
        return proofHashes.fold(ByteArray(0)) { acc, curr -> acc.plus(curr) }
    }
}
