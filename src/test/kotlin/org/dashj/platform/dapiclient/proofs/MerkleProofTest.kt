/**
 * Copyright (c) 2021-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */
package org.dashj.platform.dapiclient.proofs

import org.dashj.merk.sha256
import org.dashj.platform.dpp.toHex
import org.dashj.platform.dpp.util.Converters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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

    @Test @Disabled
    fun `getRoot should get a correct proof`() {
        val merkleTree = MerkleTree(leafHashes) { sha256(it) }

        val leafIndicesToProve = listOf(3, 4)
        val leafHashesToProve = leafIndicesToProve.map { leafIndex -> leafHashes[leafIndex] }

        val merkleProof = merkleTree.getProof(leafIndicesToProve)
        val leavesCount = merkleTree.layers[0].size

        val binaryRoot = merkleProof.calculateRoot(leafIndicesToProve, leafHashesToProve, leavesCount)

        assertEquals(expectedRootHex, binaryRoot.toHex())
    }

    @Test
    fun `verify should return true the root matches the expected root`() {
        val hexProofHashes = listOf(
            "2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6",
            "252f10c83610ebca1a059c0bae8255eba2f95be4d1d7bcfa89d7248a82d9f111",
            "e5a01fee14e0ed5c48714f22180f25ad8365b53f9779f79dc4a3d7e93963f94a",
        )

        val binaryProofHashes = hexProofHashes.map { hash -> Converters.fromHex(hash) }
        val binaryRoot = Converters.fromHex(expectedRootHex)

        val leafIndicesToProve = listOf(3, 4)
        val leafHashesToProve = leafIndicesToProve.map { leafIndex -> leafHashes[leafIndex] }

        val merkleProof = MerkleProof(binaryProofHashes) { sha256(it) }
        val leavesCount = 6

        val verified = merkleProof.verify(binaryRoot, leafIndicesToProve, leafHashesToProve, leavesCount)

        assertTrue(verified)
    }

    @Test
    fun `verify should return false if the root does not match the expected root`() {
        val merkleTree = MerkleTree(leafHashes) { sha256(it) }

        val leafIndicesToProve = listOf(3, 4)

        val merkleProof = merkleTree.getProof(leafIndicesToProve)
        val leavesCount = merkleTree.layers[0].size
        val root = merkleTree.getRoot()

        val incorrectHashes = listOf(leafHashes[1], leafHashes[4])

        val verified = merkleProof.verify(root, leafIndicesToProve, incorrectHashes, leavesCount)

        assertFalse(verified)
    }

    @Test @Disabled
    fun `getProofHashes should return correct proof hashes`() {
        val expectedProofHashes = listOf(
            "2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6",
            "252f10c83610ebca1a059c0bae8255eba2f95be4d1d7bcfa89d7248a82d9f111",
            "e5a01fee14e0ed5c48714f22180f25ad8365b53f9779f79dc4a3d7e93963f94a",
        )
        val leafIndicesToProve = listOf(3, 4)

        val merkleTree = MerkleTree(leafHashes) { sha256(it) }
        val merkleProof = merkleTree.getProof(leafIndicesToProve)

        val restoredHashes = merkleProof.proofHashes.map { it.toHex() }

        assertEquals(expectedProofHashes, restoredHashes)
    }

    @Test @Disabled
    fun `fromBytes should restore proof from bytes`() {
        val expectedFlattenedProofHashes = listOf(
            "2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6",
            "252f10c83610ebca1a059c0bae8255eba2f95be4d1d7bcfa89d7248a82d9f111",
            "e5a01fee14e0ed5c48714f22180f25ad8365b53f9779f79dc4a3d7e93963f94a"
        )
        val leafIndicesToProve = listOf(3, 4)
        val merkleTree = MerkleTree(leafHashes) { sha256(it) }
        val merkleProof = merkleTree.getProof(leafIndicesToProve)

        val bytes = merkleProof.toBytes()

        val restoredProof = MerkleProof.fromBytes(bytes) { sha256(it) }
        val restoredHashes = restoredProof.proofHashes.map { it.toHex() }

        assertEquals(expectedFlattenedProofHashes, restoredHashes)
    }

    @Test @Disabled
    fun `fromBuffer should restore proof from bytes`() {
        val expectedFlattenedProofHashes = listOf(
            "2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6",
            "252f10c83610ebca1a059c0bae8255eba2f95be4d1d7bcfa89d7248a82d9f111",
            "e5a01fee14e0ed5c48714f22180f25ad8365b53f9779f79dc4a3d7e93963f94a"
        )
        val leafIndicesToProve = listOf(3, 4)

        val merkleTree = MerkleTree(leafHashes) { sha256(it) }
        val merkleProof = merkleTree.getProof(leafIndicesToProve)

        val buffer = merkleProof.toBytes()

        val restoredProof = MerkleProof.fromBuffer(buffer) { sha256(it) }
        val restoredHashes = restoredProof.getHexProofHashes()

        assertEquals(expectedFlattenedProofHashes, restoredHashes)
    }

    @Test @Disabled
    fun `getHexProofHashes should return proof hashes serialized to hex strings`() {
        val expectedFlattenedProofHashes = listOf(
            "2e7d2c03a9507ae265ecf5b5356885a53393a2029d241394997265a1a25aefc6",
            "252f10c83610ebca1a059c0bae8255eba2f95be4d1d7bcfa89d7248a82d9f111",
            "e5a01fee14e0ed5c48714f22180f25ad8365b53f9779f79dc4a3d7e93963f94a"
        )
        val leafIndicesToProve = listOf(3, 4)

        val merkleTree = MerkleTree(leafHashes) { sha256(it) }
        val merkleProof = merkleTree.getProof(leafIndicesToProve)

        val buffer = merkleProof.toBytes()

        val restoredProof = MerkleProof.fromBuffer(buffer) { sha256(it) }
        val restoredHashes = restoredProof.getHexProofHashes()

        assertEquals(expectedFlattenedProofHashes, restoredHashes)
    }
}
