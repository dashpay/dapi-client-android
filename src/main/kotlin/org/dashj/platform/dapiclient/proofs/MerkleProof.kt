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

data class LayerInfo(val index: Int, val leavesCount: Int)

class MerkleProof(val proofHashes: List<ByteArray>, val hashFunction: (ByteArray) -> ByteArray) {

    companion object {
        @JvmStatic
        fun fromBuffer(rootTreeProof: ByteArray, hashFunction: (ByteArray) -> ByteArray): MerkleProof {
            val length = rootTreeProof.size / 32
            var offset = 0
            val proofHashes = arrayListOf<ByteArray>()
            for (i in 0 until length) {
                proofHashes.add(rootTreeProof.copyOfRange(offset, offset + 32))
                offset += 32
            }
            return MerkleProof(proofHashes, hashFunction)
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
}
