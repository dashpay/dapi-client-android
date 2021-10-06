/**
 * Copyright (c) 2021-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient.proofs

import kotlin.math.ceil
import org.dashj.platform.dapiclient.proofs.Arrays.concatAndHash
import org.dashj.platform.dapiclient.proofs.Arrays.getParentIndices
import org.dashj.platform.dapiclient.proofs.Arrays.getSiblingIndex
import org.dashj.platform.dapiclient.proofs.Arrays.getTreeDepth
import org.dashj.platform.dpp.toHex

data class ProofAccumulator(
    var currentLayerIndices: List<Int>,
    val proofHashes: List<ByteArray>
)

class MerkleTree(
    leafHashes: List<ByteArray>,
    private val hashFunction: (ByteArray) -> ByteArray
) {
    val layers: List<List<ByteArray>> = createTree(leafHashes)

    private fun calculateParentLayer(nodes: List<ByteArray>): List<ByteArray> {
        val parentLayerNodesCount = ceil(nodes.size.toDouble() / 2).toInt()
        return IntRange(0, parentLayerNodesCount - 1)
            .map { i ->
                val rightNode = if (nodes.size > (i * 2 + 1)) {
                    nodes[i * 2 + 1]
                } else {
                    null
                }
                concatAndHash(nodes[i * 2], rightNode, this.hashFunction)
            }
    }

    private fun createTree(leafHashes: List<ByteArray>): List<List<ByteArray>> {
        return IntRange(0, getTreeDepth(leafHashes.size) - 1)
            .fold(listOf(leafHashes)) { tree, layerIndex ->
                val list = arrayListOf<List<ByteArray>>()
                list.addAll(tree)
                list.addAll(listOf(calculateParentLayer(tree[layerIndex])))
                list
            }
    }

    // Public methods

    fun getRoot(): ByteArray {
        return layers[this.layers.size - 1][0]
    }

    /**
     * Returns tree depth. Tree depth is needed for the proof verification
     *
     * @return {number}
     */
    fun getDepth(): Int {
        return this.layers.size - 1
    }

    /**
     *  Returns merkle proof for the given leaf indices
     *
     * @param {number[]} leafIndices
     * @return {MerkleProof}
     */
    fun getProof(leafIndices: List<Int>): MerkleProof {
        // Proof consists of all siblings hashes that aren't in the set we're trying to prove
        // 1. Get all sibling indices. Those are the indices we need to get to the root
        // 2. Filter all nodes that doesn't require an additional hash
        // 3. Get all hashes for indices from step 2
        // 4. Remove empty spaces (the leftmost nodes that do not have anything to the right)7
        val proofHashes = arrayListOf<ByteArray>()
        val proofAccumulator = ProofAccumulator(leafIndices, proofHashes)

        val proof = layers.fold(proofAccumulator) { acc, treeLayer ->
//            proofHashes.addAll(
//                acc.currentLayerIndices.map { getSiblingIndex(it) }
//                    .filter { siblingIndex -> !acc.currentLayerIndices.contains(siblingIndex) }
//                    .map { index -> if (treeLayer.size > index) treeLayer[index] else ByteArray(0) }
//                    .filter { proofHash -> proofHash.isNotEmpty() }
//            )
            val v1 = acc.currentLayerIndices.map { getSiblingIndex(it) }
            val v2 = v1.filter { siblingIndex ->
                println("sibling index $siblingIndex in ${acc.currentLayerIndices}")
                !acc.currentLayerIndices.contains(siblingIndex)
            }
            val v3 = v2.map { index ->
                println("$index of tree ${treeLayer.size}")
                if (treeLayer.size > index) treeLayer[index] else ByteArray(0)
            }
            val v4 = v3.filter { proofHash -> proofHash.isNotEmpty() }
            acc.currentLayerIndices = getParentIndices(acc.currentLayerIndices)
            proofHashes.addAll(v4)
            acc
        }

        return MerkleProof(proof.proofHashes, this.hashFunction)
    }

    /**
     * Get tree layers as an array of hex hashes
     *
     * @return {string[][]}
     */
    fun getHexLayers(): List<List<String>> {
        return layers.map { layer ->
            layer.map { it.toHex() }
        }
    }
}
