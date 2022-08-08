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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MerkleTreeTest {
    private lateinit var leafValues: List<String>
    private lateinit var leafHashes: List<ByteArray>
    private lateinit var expectedRootHex: String

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

    @Test @Disabled
    fun `getRoot should get a correct proof`() {
        // The first layer should be siblings of leaves 3 and 4, which are leaves 2 and 5
        // Since there are 6 leaves, the second layer consists of 3 nodes, 2 of which we
        // can now figure out from the first layer:
        // (node1 = hash(leaf2 + leaf3), node2 = hash(leaf4 + leaf 5)). So from the
        // second layer we need node0, and nothing from the top layer - 2 hashes from
        // there we will be able to calculate from the information we have.

        val expectedProofHashes = listOf(
            "2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6",
            "252f10c83610ebca1a059c0bae8255eba2f95be4d1d7bcfa89d7248a82d9f111",
            "e5a01fee14e0ed5c48714f22180f25ad8365b53f9779f79dc4a3d7e93963f94a"
        )

        val leafIndicesToProve = listOf(3, 4)
        // val leafHashesToProve = leafIndicesToProve.map { leafIndex -> leafHashes[leafIndex] }

        val merkleTree = MerkleTree(leafHashes) { sha256(it) }

        val merkleProof = merkleTree.getProof(leafIndicesToProve)

        val hexLayers = merkleProof.proofHashes.map {
            it.toHex()
        }

        assertEquals(expectedProofHashes, hexLayers)
    }

    @Test @Disabled
    fun `getRoot should get correct proof leaves 1 and 3`() {
        val expectedProofHashes = listOf(
            "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
            "2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6",
            "04fa33f8b4bd3db545fa04cdd51b462509f611797c7bfe5c944ee2bb3b2ed908"
        )

        val leafIndicesToProve = listOf(1, 3)

        val merkleTree = MerkleTree(leafHashes) { sha256(it) }
        val merkleProof = merkleTree.getProof(leafIndicesToProve)

        val hexLayers = merkleProof.proofHashes.map {
            it.toHex()
        }
        assertEquals(expectedProofHashes, hexLayers)
    }

    @Test
    fun `#getTreeDepth should return a correct tree depth`() {
        // 6 leaves - 3 layers deep tree. Tree depth generally can be figure out
        val merkleTree = MerkleTree(leafHashes) { sha256(it) }

        assertEquals(3, merkleTree.getDepth())
    }

    @Test
    fun `getHexLayers should return all tree nodes converted to hex strings`() {
        val merkleTree = MerkleTree(leafHashes) { sha256(it) }

        assertEquals(
            merkleTree.getHexLayers(),
            listOf(
                listOf(
                    "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
                    "3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d",
                    "2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6",
                    "18ac3e7343f016890c510e93f935261169d9e3f565436429830faf0934f4f8e4",
                    "3f79bb7b435b05321651daefd374cdc681dc06faa65e374e38337b88ca046dea",
                    "252f10c83610ebca1a059c0bae8255eba2f95be4d1d7bcfa89d7248a82d9f111"
                ),
                listOf(
                    "e5a01fee14e0ed5c48714f22180f25ad8365b53f9779f79dc4a3d7e93963f94a",
                    "bffe0b34dba16bc6fac17c08bac55d676cded5a4ade41fe2c9924a5dde8f3e5b",
                    "04fa33f8b4bd3db545fa04cdd51b462509f611797c7bfe5c944ee2bb3b2ed908"
                ),
                listOf(
                    "14ede5e8e97ad9372327728f5099b95604a39593cac3bd38a343ad76205213e7",
                    "04fa33f8b4bd3db545fa04cdd51b462509f611797c7bfe5c944ee2bb3b2ed908"
                ),
                listOf(
                    "1f7379539707bcaea00564168d1d4d626b09b73f8a2a365234c62d763f854da2"
                )
            )
        )
    }
}
