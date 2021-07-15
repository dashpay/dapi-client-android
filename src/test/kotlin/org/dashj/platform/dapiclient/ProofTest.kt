package org.dashj.platform.dapiclient

import org.bitcoinj.core.Context
import org.bitcoinj.params.SchnappsDevNetParams
import org.dashj.platform.dapiclient.model.DocumentQuery
import org.dashj.platform.dpp.identifier.Identifier
import org.dashj.platform.dpp.toHexString
import org.dashj.platform.dpp.util.HashUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ProofTest {

    val PARAMS = SchnappsDevNetParams.get()
    val CONTEXT = Context.getOrCreate(PARAMS)
    val masternodeList = PARAMS.defaultMasternodeList.toList()
    val dpnsContractId = Identifier.from("4BrYpaW5s26UWoBk9zEAYWxJANX7LFinmToprWo3VwgS") // DPNS contract
    val dashPayContractId = Identifier.from("9FmdUoXZJijvARgA3Vcg73ThYp5P4AaLis1WpXp9VGg1")
    val identityId = Identifier.from("FrdbRMnZ5pPiFWuzPR62goRVj6sxpqvLKMT87ZmuZPyr")
    val badDpnsContractId = Identifier.from("5BrYpaW5s26UWoBk9zEAYWxJANX7LFinmToprWo3VwgS") // DPNS contract
    val badDashPayContractId = Identifier.from("8FmdUoXZJijvARgA3Vcg73ThYp5P4AaLis1WpXp9VGg1")
    val badIdentityId = Identifier.from("GrdbRMnZ5pPiFWuzPR62goRVj6sxpqvLKMT87ZmuZPyr")
    val publicKeyHash = HashUtils.fromHex("fa396d727565f94d26f85e7f8a4fe5418f97d7cb")
    val publicKeyHashes = listOf(publicKeyHash, HashUtils.fromHex("aad3374c8aa0059809d677bcb44c86d4e7746bb8"))

    val badPublicKeyHash = HashUtils.fromHex("ea396d727565f94d26f85e7f8a4fe5418f97d7cb")
    val badPublicKeyHashes = listOf(badPublicKeyHash, HashUtils.fromHex("bad3374c8aa0059809d677bcb44c86d4e7746bb9"))

    val stateRepository = StateRepositoryMock()

    @Test
    fun getIdentityWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val identityBytes = client.getIdentity(identityId.toBuffer(), false)!!
            println("identity: ${identityBytes.toByteArray().toHexString()}")

            val identityBytesWithProof = client.getIdentity(identityId.toBuffer(), true)!!
            println("identity: ${identityBytesWithProof.toByteArray().toHexString()}")

            val badIdentityBytes = client.getIdentity(badIdentityId.toBuffer(), false)
            println("contract: $badIdentityBytes")

            val badIdentityBytesWithProof = client.getIdentity(badIdentityId.toBuffer(), true)
            println("contract: $badIdentityBytesWithProof")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getDocumentsWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val query = DocumentQuery.Builder()
                .where(listOf("normalizedParentDomainName", "==", "dash").toMutableList())
                .build()
            val documents = client.getDocuments(dpnsContractId.toBuffer(), "domain", query, false)
            println("documents: ${documents.size}")

            val documentsWithProof = client.getDocuments(dpnsContractId.toBuffer(), "domain", query, true)
            println("documents: ${documentsWithProof.size}")

            val badQuery = DocumentQuery.Builder()
                .where(listOf("normalizedParentDomainName", "==", "dash").toMutableList())
                .where(listOf("normalizedLabel", "startsWith", "8z9y7z").toMutableList())
                .build()
            val badDocuments = client.getDocuments(dpnsContractId.toBuffer(), "domain", badQuery, false)
            println("documents: ${badDocuments.size}")

            val badDocumentsWithProof = client.getDocuments(dpnsContractId.toBuffer(), "domain", badQuery, true)
            println("documents: ${badDocumentsWithProof.size}")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getContractsWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val contractBytes = client.getDataContract(dpnsContractId.toBuffer(), false)!!
            println("contract: ${contractBytes.toByteArray().toHexString()}")

            val constractBytesWithProof = client.getDataContract(dpnsContractId.toBuffer(), true)!!
            println("contract: ${constractBytesWithProof.toByteArray().toHexString()}")

            val badContractBytes = client.getDataContract(badDpnsContractId.toBuffer(), false)
            println("contract: $badContractBytes")

            val badConstractBytesWithProof = client.getDataContract(badDpnsContractId.toBuffer(), true)
            println("contract: $badConstractBytesWithProof")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getFirstIdentityWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val identity = client.getIdentityByFirstPublicKey(publicKeyHash, false)!!
            println("identity id: ${identity.toByteArray().toHexString()}")

            val identityWithProof = client.getIdentityByFirstPublicKey(publicKeyHash, true)!!
            println("identity id: ${identityWithProof.toByteArray().toHexString()}")

            val badIdentities = client.getIdentityByFirstPublicKey(badPublicKeyHash, false)
            println("identity: $badIdentities")

            val badIdentitiesWithProof = client.getIdentityByFirstPublicKey(badPublicKeyHash, true)
            println("identity: $badIdentitiesWithProof")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getIdentityIdWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val identityIds = client.getIdentityIdByFirstPublicKey(publicKeyHash, false)!!
            println("identity id: ${identityIds.toByteArray().toHexString()}")

            val identityIdsWithProof = client.getIdentityIdByFirstPublicKey(publicKeyHash, true)!!
            println("identity id: ${identityIdsWithProof.toByteArray().toHexString()}")

            val badIdentityIds = client.getIdentityIdByFirstPublicKey(badPublicKeyHash, false)
            println("identity id: $badIdentityIds")

            val badIdentityIdsWithProof = client.getIdentityIdByFirstPublicKey(badPublicKeyHash, true)
            println("identity id: $badIdentityIdsWithProof")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getIdentityIdsWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val identityIds = client.getIdentityIdsByPublicKeyHashes(publicKeyHashes, false)!!
            println("identity id: ${identityIds.map { it.toByteArray().toHexString()}}")

            val identityIdsWithProof = client.getIdentityIdsByPublicKeyHashes(publicKeyHashes, true)!!
            println("identity id: ${identityIdsWithProof.map { it.toByteArray().toHexString()}}")

            val badIdentityIds = client.getIdentityIdsByPublicKeyHashes(badPublicKeyHashes, false)
            println("identity id: $badIdentityIds")

            val badIdentityIdsWithProof = client.getIdentityIdsByPublicKeyHashes(badPublicKeyHashes, true)
            println("identity id: $badIdentityIdsWithProof")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getIdentitiesWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val identityIds = client.getIdentitiesByPublicKeyHashes(publicKeyHashes, false)!!
            println("identities: ${identityIds.map { it.toByteArray().toHexString()}}")

            val identityIdsWithProof = client.getIdentitiesByPublicKeyHashes(publicKeyHashes, true)!!
            println("identities: ${identityIdsWithProof.map { it.toByteArray().toHexString()}}")

            val badIdentityIds = client.getIdentitiesByPublicKeyHashes(badPublicKeyHashes, false)
            println("identities: $badIdentityIds")

            val badIdentityIdsWithProof = client.getIdentitiesByPublicKeyHashes(badPublicKeyHashes, true)
            println("identities: $badIdentityIdsWithProof")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }
}
