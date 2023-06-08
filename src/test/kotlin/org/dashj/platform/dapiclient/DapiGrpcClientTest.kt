package org.dashj.platform.dapiclient

import com.google.common.base.Stopwatch
import com.hashengineering.crypto.X11
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Block
import org.bitcoinj.core.Context
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bitcoinj.params.DevNetParams
import org.bitcoinj.params.TestNet3Params
import org.dashj.platform.dapiclient.errors.NotFoundException
import org.dashj.platform.dapiclient.model.DocumentQuery
import org.dashj.platform.dapiclient.provider.DAPIAddress
import org.dashj.platform.dapiclient.provider.ListDAPIAddressProvider
import org.dashj.platform.dpp.DashPlatformProtocol
import org.dashj.platform.dpp.document.Document
import org.dashj.platform.dpp.identifier.Identifier
import org.dashj.platform.dpp.toHex
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class DapiGrpcClientTest : BaseTest() {

    val PARAMS = TestNet3Params.get()
    val CONTEXT = Context.getOrCreate(PARAMS)
    val masternodeList = PARAMS.defaultHPMasternodeList.toList()
    val dpnsContractId = SystemIds.dpnsDataContractId // DPNS contract
    val dashPayContractId = SystemIds.dashpayDataContractId
    val identityId = SystemIds.dpnsOwnerId
    val badDpnsContractId = Identifier.from("5BrYpaW5s26UWoBk9zEAYWxJANX7LFinmToprWo3VwgS") // DPNS contract
    val badDashPayContractId = Identifier.from("8FmdUoXZJijvARgA3Vcg73ThYp5P4AaLis1WpXp9VGg1")

    val identityIdString = SystemIds.dpnsOwnerId.toString()
    val stateRepository = StateRepositoryMock()
    val dpp = DashPlatformProtocol(stateRepository)

    // default client used in most unit tests
    private val client = DapiClient(masternodeList, dpp)

    @Test
    fun getStatusOfInvalidNodeTest() {
        val watch = Stopwatch.createStarted()
        val list = ListDAPIAddressProvider(listOf("211.30.243.83").map { DAPIAddress(it) }, 0)
        val client = DapiClient(list, dpp, 3000, 0, 3)
        try {
            client.getStatus(DAPIAddress("211.30.243.82"), 0)
            fail<Nothing>("The node queried should not exist")
        } catch (e: Exception) {
            if (e is NoAvailableAddressesForRetryException || e is MaxRetriesReachedException) {
                println("timeout after $watch")
                val cause = e.cause as StatusRuntimeException
                if (cause.status.code != Status.UNAVAILABLE.code && cause.status.code != Status.DEADLINE_EXCEEDED.code) {
                    fail<Nothing>("Invalid node test failed with a different error")
                }
                println(client.reportNetworkStatus())
            }
        }
    }

    @Test
    fun getStatusTest() {
        try {
            val status = client.getStatus()
            println(status)
        } finally {
        }
    }

    @Test
    fun getBlockTest() {
        val nodes = listOf("127.0.0.1", masternodeList[-0])
        val client = DapiClient(nodes, dpp)
        try {
            val block1 = when (PARAMS) {
                is DevNetParams -> (PARAMS as DevNetParams).devNetGenesisBlock
                is TestNet3Params -> Block(PARAMS, Utils.HEX.decode("020000002cbcf83b62913d56f605c0e581a48872839428c92e5eb76cd7ad94bcaf0b00007f11dcce14075520e8f74cc4ddf092b4e26ebd23b8d8665a1ae5bfc41b58fdb4c3a95e53ffff0f1ef37a00000101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0a510101062f503253482fffffffff0100743ba40b0000002321020131f38ae3eb0714531dbfc3f45491b4131d1211e3777177636388bb5a74c3e4ac00000000"))
                else -> fail("Invalid network")
            }
            val block1data = block1.bitcoinSerialize().toHex()
            val block1Hash = block1.hashAsString

            // request the block from the height
            val blockFromHeight = client.getBlockByHeight(1)
            assertEquals(block1data, blockFromHeight!!.toHex())

            // hash the block header and compare to the actual value
            val hash = Sha256Hash.wrapReversed(X11.x11Digest(blockFromHeight!!.take(80).toByteArray()))
            assertEquals(block1Hash, hash.toString())

            // request the block from the hash and compare to the block obtained from the height
            val blockFromHash = client.getBlockByHash(block1Hash)
            assertEquals(blockFromHeight.toHex(), blockFromHash!!.toHex())
        } finally {
        }
    }

    @Test
    fun getDPNSContractTest() {
        try {
            val contractResponse = client.getDataContract(dpnsContractId.toBuffer())

            val contract = dpp.dataContract.createFromBuffer(contractResponse.dataContract)
            val jsonDpnsFile = File("src/test/resources/dpns-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = dpp.dataContract.createFromObject(rawContract)

            assertEquals(dpnsContract.toJSON(), contract.toJSON())
        } finally {
        }
    }

    @Test
    fun getDashPayContractTest() {
        try {
            val contractResponse = client.getDataContract(dashPayContractId.toBuffer())

            val contract = dpp.dataContract.createFromBuffer(contractResponse.dataContract)
            val jsonDpnsFile = File("src/test/resources/dashpay-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = dpp.dataContract.createFromObject(rawContract)

            assertEquals(dpnsContract.toJSON(), contract.toJSON())
        } finally {
        }
    }

    @Test
    fun getNonExistantContract() {
        val client = DapiClient(masternodeList.toList(), dpp)
        val contractId = Base58.decode("88w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3")
        try {
            assertThrows(NotFoundException::class.java) { client.getDataContract(contractId) }
        } finally {
        }
    }

    @Test
    fun getDocumentsTest() {
        val client = DapiClient(masternodeList.toList(), dpp)
        try {
            val query = DocumentQuery.Builder()
                .where("normalizedParentDomainName", "==", "dash")
                .orderBy("normalizedLabel", true)
                .build()
            val documentsResponse = client.getDocuments(dpnsContractId.toBuffer(), "domain", query)

            val jsonDpnsFile = File("src/test/resources/dpns-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = dpp.dataContract.createFromObject(rawContract)
            stateRepository.storeDataContract(dpnsContract)
            val document = dpp.document.createFromBuffer(documentsResponse.documents[0])

            val docs = ArrayList<Document>(documentsResponse.documents.size)
            for (doc in documentsResponse.documents) {
                docs.add(dpp.document.createFromBuffer(doc))
            }

            println(document.toJSON())
        } finally {
        }
    }

    @Test
    fun getDocumentsStartAtTest() {
        val client = DapiClient(masternodeList.toList(), dpp)
        try {
            val query = DocumentQuery.Builder()
                .build()
            val documentsResponse = client.getDocuments(dpnsContractId.toBuffer(), "domain", query)

            val jsonDpnsFile = File("src/test/resources/dpns-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = dpp.dataContract.createFromObject(rawContract)
            stateRepository.storeDataContract(dpnsContract)
            val document = dpp.document.createFromBuffer(documentsResponse.documents[0])
            println(document.toJSON())

            val docs = ArrayList<Document>(documentsResponse.documents.size)
            for (doc in documentsResponse.documents) {
                docs.add(dpp.document.createFromBuffer(doc))
            }

            val queryWithStartAt = DocumentQuery.Builder()
                .startAt(docs.first().id)
                .build()

            val documentsStartAtResponse = client.getDocuments(dpnsContractId.toBuffer(), "domain", queryWithStartAt)

            val docsStartAt = ArrayList<Document>(documentsResponse.documents.size)
            for (doc in documentsStartAtResponse.documents) {
                docsStartAt.add(dpp.document.createFromBuffer(doc))
            }
            assertEquals(queryWithStartAt.startAt, docsStartAt.first().id)
        } finally {
        }
    }

    @Test
    fun getIdentityFromBadPubKeyBytes() {
        val key = ECKey()

        val identity = client.getIdentityByFirstPublicKey(key.pubKeyHash)
        assertEquals(null, identity)

        val identitiesResponse = client.getIdentitiesByPublicKeyHashes(listOf(key.pubKeyHash))
        assertEquals(0, identitiesResponse.identities.size)
        assertNull(identitiesResponse.identities.firstOrNull())
    }

    @Test
    fun getTransationTest() {
        val txid = "7609edb70ffe29ccad3054858abab0379965878bcafe18cf76dd80a6ccccf63d"

        val result = client.getTransaction(txid)

        println(result)

        val resultTwo = client.getTransactionBytes(txid)

        println(resultTwo)
    }

    @Test
    fun getDocumentsFailureTest() {
        val query = DocumentQuery.Builder()
            .where("normalizedParentDomainName", "==", "dash")
            .where("normalizedLabel", "==", "hash")
            .build()
        val response = client.getDocuments(dpnsContractId.toBuffer(), "domain", query, false)
        println("documents: ${response.documents.size}")

        val badContractQuery = DocumentQuery.Builder()
            .where("normalizedParentDomainName", "==", "dash")
            .where("normalizedLabel", "startsWith", "RT-")
            .orderBy("normalizedLabel", true)
            .build()
        val badContractResponse = client.getDocuments(dpnsContractId.toBuffer(), "domain", badContractQuery, false)
        println("documents: ${badContractResponse.documents.size}")
    }

    @Test
    fun getDocumentsByQueryTests() {
        client.getDocuments(
            dashPayContractId.toBuffer(),
            "profile",
            DocumentQuery.builder()
                .where("\$ownerId", "==", Identifier.from("3HSUPuMgR5qpZt1y5NbE2BBheM11yLRXKZoqdsKgxVNt"))
                .where("\$updatedAt", ">", 0)
                .orderBy("\$updatedAt", true)
                .build()
        )

        client.getDocuments(
            dpnsContractId.toBuffer(),
            "domain",
            DocumentQuery.builder()
                .where("normalizedParentDomainName", "==", "dash")
                .where("normalizedLabel", "startsWith", "test")
                .orderBy("normalizedLabel", true)
                .build()
        )

        // orderBy with two operators
        client.getDocuments(
            dpnsContractId.toBuffer(),
            "domain",
            DocumentQuery.builder()
                .where("normalizedParentDomainName", "==", "dash")
                .where("normalizedLabel", "startsWith", "test")
                .orderBy("normalizedLabel", true)
                .build()
        )

        // as of 0.22-dev-7 this query should fail
        // where clauses must be in a particular order
        // as of 0.23 this query will be successful
        client.getDocuments(
            dpnsContractId.toBuffer(),
            "domain",
            DocumentQuery.builder()
                .where("normalizedLabel", "startsWith", "test")
                .where("normalizedParentDomainName", "==", "dash")
                .orderBy("normalizedLabel", true)
                .build()
        )

        // use the reverse order as the previous query
        client.getDocuments(
            dpnsContractId.toBuffer(),
            "domain",
            DocumentQuery.builder()
                .where("normalizedParentDomainName", "==", "dash")
                .where("normalizedLabel", "startsWith", "test")
                .orderBy("normalizedLabel", true)
                .build()
        )

        // as of 0.22-dev-7 this query should fail
        // multiple ranges are not supported
        // in 0.23, this still fails
        // INVALID_ARGUMENT: Invalid query: where clause on non indexed property error: query must be for valid indexes
        assertThrows<StatusRuntimeException> {
            client.getDocuments(
                dashPayContractId.toBuffer(),
                "profile",
                DocumentQuery.builder()
                    .where("\$ownerId", "in", Identifier.from("3HSUPuMgR5qpZt1y5NbE2BBheM11yLRXKZoqdsKgxVNt"))
                    .where("\$updatedAt", ">", 0)
                    .orderBy("\$updatedAt")
                    .orderBy("\$ownerId")
                    .build()
            )
        }

        client.getDocuments(
            dpnsContractId.toBuffer(),
            "preorder",
            DocumentQuery.builder()
                .where("saltedDomainHash", "==", Sha256Hash.ZERO_HASH.bytes)
                .build()
        )
    }
}
