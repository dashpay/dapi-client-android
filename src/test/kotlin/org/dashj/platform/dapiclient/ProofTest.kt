package org.dashj.platform.dapiclient

import org.bitcoinj.core.Context
import org.bitcoinj.params.SchnappsDevNetParams
import org.dashj.merk.MerkVerifyProof
import org.dashj.platform.dapiclient.errors.NotFoundException
import org.dashj.platform.dapiclient.model.DocumentQuery
import org.dashj.platform.dpp.identifier.Identifier
import org.dashj.platform.dpp.toHex
import org.dashj.platform.dpp.util.Converters
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ProofTest {

    init {
        MerkVerifyProof.init()
    }

    val PARAMS = SchnappsDevNetParams.get()
    val CONTEXT = Context.getOrCreate(PARAMS)
    val masternodeList = PARAMS.defaultMasternodeList.toList()
    val dpnsContractId = Identifier.from("8F4WqzVuqyYEBMR1AraBuYG1cjk3hqUYdzLSMdYpWLbH") // DPNS contract
    val dashPayContractId = Identifier.from("8Mpj1D1fSg1AtKoCFBcSg7UTfqChpFTHLQBPVrXUACxC")
    val identityId = Identifier.from("5DSwyjJiUXArZ42Uj1g7wSPLgWTBYCT3p4pHPmtEDH5b")
    val badDpnsContractId = Identifier.from("5BrYpaW5s26UWoBk9zEAYWxJANX7LFinmToprWo3VwgS") // DPNS contract
    val badDashPayContractId = Identifier.from("8FmdUoXZJijvARgA3Vcg73ThYp5P4AaLis1WpXp9VGg1")
    val badIdentityId = Identifier.from("GrdbRMnZ5pPiFWuzPR62goRVj6sxpqvLKMT87ZmuZPyr")
    val publicKeyHash = Converters.fromHex("106d620310b02fe89f3c103706f3e23ffad4b680")
    val publicKeyHashes = listOf(publicKeyHash, Converters.fromHex("106d620310b02fe89f3c103706f3e23ffad4b680"))

    val badPublicKeyHash = Converters.fromHex("ea396d727565f94d26f85e7f8a4fe5418f97d7cb")
    val badPublicKeyHashes = listOf(badPublicKeyHash, Converters.fromHex("bad3374c8aa0059809d677bcb44c86d4e7746bb9"))

    val stateRepository = StateRepositoryMock()

    @Test
    fun getIdentityWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val identityBytes = client.getIdentity(identityId.toBuffer(), false)!!
            println("identity: ${identityBytes.identity.toHex()}")

            val identityBytesWithProof = client.getIdentity(identityId.toBuffer(), true)!!
            println("identity: ${identityBytesWithProof.identity.toHex()}")

            try {
                client.getIdentity(badIdentityId.toBuffer(), false)
            } catch (e: NotFoundException) {
                println(e.message)
            }

            try {
                client.getIdentity(badIdentityId.toBuffer(), true)
            } catch (e: NotFoundException) {
                println(e.message)
            }
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
            val documentsResponse = client.getDocuments(dpnsContractId.toBuffer(), "domain", query, false)
            println("documents: ${documentsResponse.documents.size}")

            val documentsWithProof = client.getDocuments(dpnsContractId.toBuffer(), "domain", query, true)
            println("documents: ${documentsWithProof.documents.size}")

            val badQuery = DocumentQuery.Builder()
                .where(listOf("normalizedParentDomainName", "==", "dash").toMutableList())
                .where(listOf("normalizedLabel", "startsWith", "8z9y7z").toMutableList())
                .build()
            val badDocumentResponse = client.getDocuments(dpnsContractId.toBuffer(), "domain", badQuery, false)
            println("documents: ${badDocumentResponse.documents.size}")

            val badDocumentsResponseWithProof = client.getDocuments(dpnsContractId.toBuffer(), "domain", badQuery, true)
            println("documents: ${badDocumentsResponseWithProof.documents.size}")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getContractsWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val contractBytes = client.getDataContract(dpnsContractId.toBuffer(), false)!!
            println("contract: ${contractBytes.dataContract}")

            val constractBytesWithProof = client.getDataContract(dpnsContractId.toBuffer(), true)!!
            println("contract: ${constractBytesWithProof.dataContract}")

            try {
                client.getDataContract(badDpnsContractId.toBuffer(), false)
            } catch (e: NotFoundException) {
                println(e.message)
            }

            try {
                client.getDataContract(badDpnsContractId.toBuffer(), true)
            } catch (e: NotFoundException) {
                println(e.message)
            }
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getFirstIdentityWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val identity = client.getIdentityByFirstPublicKey(publicKeyHash, false)!!
            println("identity id: ${identity.toByteArray().toHex()}")

            val identityWithProof = client.getIdentityByFirstPublicKey(publicKeyHash, true)!!
            println("identity id: ${identityWithProof.toByteArray().toHex()}")

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
            println("identity id: ${identityIds.toByteArray().toHex()}")

            val identityIdsWithProof = client.getIdentityIdByFirstPublicKey(publicKeyHash, true)!!
            println("identity id: ${identityIdsWithProof.toByteArray().toHex()}")

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
            val response = client.getIdentityIdsByPublicKeyHashes(publicKeyHashes, false)!!
            println("identity ids: ${response.identityIds.map { it.toHex()}}")

            val responseWithProof = client.getIdentityIdsByPublicKeyHashes(publicKeyHashes, true)!!
            println("identity ids: ${responseWithProof.identityIds.map { it.toHex()}}")

            val badResponse = client.getIdentityIdsByPublicKeyHashes(badPublicKeyHashes, false)
            println("identity ids: ${badResponse.identityIds.size}")

            val badResponseWithProof = client.getIdentityIdsByPublicKeyHashes(badPublicKeyHashes, true)
            println("identity ids: ${badResponseWithProof.identityIds.size}")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getIdentitiesWithProof() {
        val client = DapiClient(masternodeList.toList())
        try {
            val response = client.getIdentitiesByPublicKeyHashes(publicKeyHashes, false)!!
            println("identities: ${response.identities.map { it.toHex()}}")

            val responseWithProof = client.getIdentitiesByPublicKeyHashes(publicKeyHashes, true)!!
            println("identities: ${responseWithProof.identities.map { it.toHex()}}")

            val badResponse = client.getIdentitiesByPublicKeyHashes(badPublicKeyHashes, false)
            println("identities: $badResponse")

            val badResponseWithProof = client.getIdentitiesByPublicKeyHashes(badPublicKeyHashes, true)
            println("identities: $badResponseWithProof")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }
}
