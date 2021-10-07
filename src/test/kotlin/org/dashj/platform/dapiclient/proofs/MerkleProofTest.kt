/**
 * Copyright (c) 2021-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.proofs

import org.dashj.merk.sha256
import org.dashj.platform.dpp.toHex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MerkleProofTest {
    lateinit var leafValues: List<String>
    lateinit var leafHashes: List<ByteArray>
    lateinit var expectedRootHex: String

    @BeforeEach
    fun beforeEach() {
        leafValues = listOf("a", "b", "c", "d", "e", "f")
        leafHashes = leafValues.map { x -> sha256(x.toByteArray(Charsets.UTF_8)) }

        expectedRootHex = "1f7379539707bcaea00564168d1d4d626b09b73f8a2a365234c62d763f854da2"
    }

    @Test
    fun `first getRoot should get a correct root`() {
        val merkleTree = MerkleTree(leafHashes) { sha256(it) }

        val hexRoot = merkleTree.getRoot().toHex()

        assertEquals(expectedRootHex, hexRoot)
    }

    @Test
    fun `getRoot should get a correct root`() {
        val merkleTree = MerkleTree(leafHashes) { sha256(it) }

        val leafIndicesToProve = listOf(3, 4)
        val leafHashesToProve = leafIndicesToProve.map { leafIndex -> leafHashes[leafIndex] }

        val merkleProof = merkleTree.getProof(leafIndicesToProve)
        val leavesCount = merkleTree.layers[0].size

        val binaryRoot = merkleProof.calculateRoot(leafIndicesToProve, leafHashesToProve, leavesCount)

        val hexRoot = binaryRoot.toHex()

        assertEquals(expectedRootHex, hexRoot)
    }
}
