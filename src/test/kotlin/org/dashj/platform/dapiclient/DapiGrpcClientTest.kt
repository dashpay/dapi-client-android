package org.dashj.platform.dapiclient

import com.google.common.base.Stopwatch
import com.hashengineering.crypto.X11
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.io.File
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Block
import org.bitcoinj.core.Context
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bitcoinj.params.DevNetParams
import org.bitcoinj.params.KrupnikDevNetParams
import org.bitcoinj.params.TestNet3Params
import org.dashj.platform.dapiclient.errors.NotFoundException
import org.dashj.platform.dapiclient.model.DocumentQuery
import org.dashj.platform.dapiclient.provider.DAPIAddress
import org.dashj.platform.dapiclient.provider.ListDAPIAddressProvider
import org.dashj.platform.dpp.DashPlatformProtocol
import org.dashj.platform.dpp.contract.ContractFactory
import org.dashj.platform.dpp.document.Document
import org.dashj.platform.dpp.document.DocumentFactory
import org.dashj.platform.dpp.identity.IdentityFactory
import org.dashj.platform.dpp.toHexString
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class DapiGrpcClientTest {

    val PARAMS = KrupnikDevNetParams.get()
    val CONTEXT = Context.getOrCreate(PARAMS)
    val masternodeList = PARAMS.defaultMasternodeList.toList()
    val dpnsContractId = Base58.decode("EBnvxB5RSW8NbBbXdRS3zPqFEaZnjCZ8WeurjvLTaru7") // DPNS contract
    val dashPayContractId = Base58.decode("GAvZdha4t3mCQyvCVvv7LMYw3CXN4X5hvFZ4S6qCAdod")
    val identityIdString = "EBnvxB5RSW8NbBbXdRS3zPqFEaZnjCZ8WeurjvLTaru7"
    val stateRepository = StateRepositoryMock()
    val dpp = DashPlatformProtocol(stateRepository)
    val contractFactory = ContractFactory(dpp, stateRepository)
    val identityFactory = IdentityFactory(dpp, stateRepository)
    private val documentFactory = DocumentFactory(dpp, stateRepository)

    @Test
    fun getStatusOfInvalidNodeTest() {
        val watch = Stopwatch.createStarted()
        val list = ListDAPIAddressProvider(listOf("211.30.243.83").map { DAPIAddress(it) }, 0)
        val client = DapiClient(list, 3000, 0, 3)
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
        val client = DapiClient(masternodeList)
        try {
            val status = client.getStatus()
            println(status)
        } finally {
        }
    }

    @Test
    fun getBlockTest() {
        val nodes = listOf("127.0.0.1", masternodeList[-0])
        val client = DapiClient(nodes)
        try {
            val block1 = when (PARAMS) {
                is DevNetParams -> (PARAMS as DevNetParams).devNetGenesisBlock
                is TestNet3Params -> Block(PARAMS, Utils.HEX.decode("020000002cbcf83b62913d56f605c0e581a48872839428c92e5eb76cd7ad94bcaf0b00007f11dcce14075520e8f74cc4ddf092b4e26ebd23b8d8665a1ae5bfc41b58fdb4c3a95e53ffff0f1ef37a00000101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0a510101062f503253482fffffffff0100743ba40b0000002321020131f38ae3eb0714531dbfc3f45491b4131d1211e3777177636388bb5a74c3e4ac00000000"))
                else -> fail("Invalid network")
            }
            val block1data = block1.bitcoinSerialize().toHexString()
            val block1Hash = block1.hashAsString

            // request the block from the height
            val blockFromHeight = client.getBlockByHeight(1)
            assertEquals(block1data, blockFromHeight!!.toHexString())

            // hash the block header and compare to the actual value
            val hash = Sha256Hash.wrapReversed(X11.x11Digest(blockFromHeight!!.take(80).toByteArray()))
            assertEquals(block1Hash, hash.toString())

            // request the block from the hash and compare to the block obtained from the height
            val blockFromHash = client.getBlockByHash(block1Hash)
            assertEquals(blockFromHeight.toHexString(), blockFromHash!!.toHexString())
        } finally {
        }
    }

    @Test
    fun getDPNSContractTest() {
        val client = DapiClient(masternodeList)
        try {
            val contractResponse = client.getDataContract(dpnsContractId)

            val contract = contractFactory.createFromBuffer(contractResponse.dataContract)
            val jsonDpnsFile = File("src/test/resources/dpns-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = contractFactory.createFromObject(rawContract)

            assertEquals(dpnsContract.toJSON(), contract.toJSON())
        } finally {
        }
    }

    @Test
    fun getDashPayContractTest() {
        val client = DapiClient(masternodeList)
        try {
            val contractResponse = client.getDataContract(dashPayContractId)

            val contract = contractFactory.createFromBuffer(contractResponse.dataContract)
            val jsonDpnsFile = File("src/test/resources/dashpay-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = contractFactory.createFromObject(rawContract)

            assertEquals(dpnsContract.toJSON(), contract.toJSON())
        } finally {
        }
    }

    @Test
    fun getNonExistantContract() {
        val client = DapiClient(masternodeList.toList())
        val contractId = Base58.decode("88w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3")
        try {
            // val contractBytes = client.getDataContract(contractId)
            assertThrows(NotFoundException::class.java) { client.getDataContract(contractId) }
        } finally {
        }
    }

    @Test
    fun getDocumentsTest() {
        val client = DapiClient(masternodeList.toList())
        try {
            val query = DocumentQuery.Builder()
                .where(listOf("normalizedParentDomainName", "==", "dash").toMutableList())
                // .where(listOf("normalizedLabel", "startsWith", "test").toMutableList())
                .build()
            val documentsResponse = client.getDocuments(dpnsContractId, "domain", query)
            println(documentsResponse.documents[0].toHexString())

            val jsonDpnsFile = File("src/test/resources/dpns-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = contractFactory.createFromObject(rawContract)
            stateRepository.storeDataContract(dpnsContract)
            val document = documentFactory.createFromBuffer(documentsResponse.documents[0])

            val docs = ArrayList<Document>(documentsResponse.documents.size)
            for (doc in documentsResponse.documents) {
                docs.add(documentFactory.createFromBuffer(doc))
            }

            println(document.toJSON())
        } finally {
        }
    }

    @Test
    fun getIdentityTest() {
        val id = Base58.decode(identityIdString)
        val badId = Base58.decode(identityIdString.replace(identityIdString[0], '3'))
        val client = DapiClient(masternodeList)
        val identityBytes = client.getIdentity(id)
        assertThrows(NotFoundException::class.java) {
            client.getIdentity(badId)
        }
        val identity = identityFactory.createFromBuffer(identityBytes.identity)
        println(JSONObject(identity.toJSON()).toString(2))

        val pubKeyHash = ECKey.fromPublicOnly(identity.getPublicKeyById(0)!!.data).pubKeyHash
        val identityByPublicKeyBytes = client.getIdentityByFirstPublicKey(pubKeyHash)
        val identitiesByPublicKeyHashes = client.getIdentitiesByPublicKeyHashes(listOf(pubKeyHash))
        val identityByPublicKey = identityFactory.createFromBuffer(identityByPublicKeyBytes!!.toByteArray())
        val identityByPublicKeyHashes = identityFactory.createFromBuffer(identitiesByPublicKeyHashes.identities[0])
        val identityIdByPublicKey = client.getIdentityIdByFirstPublicKey(pubKeyHash)

        val identityIdsByPublicKey = client.getIdentityIdsByPublicKeyHashes(listOf(pubKeyHash))

        assertEquals(identityIdString, identityByPublicKey.id.toString())
        assertEquals(identityIdString, identityByPublicKeyHashes!!.id.toString())
        assertArrayEquals(id, identityIdByPublicKey!!.toByteArray())
        assertArrayEquals(id, identityIdsByPublicKey.identityIds[0])
    }

    @Test
    fun getIdentityFromBadPubKeyBytes() {
        val key = ECKey()
        val client = DapiClient(masternodeList)

        val identity = client.getIdentityByFirstPublicKey(key.pubKeyHash)
        assertEquals(null, identity)

        val identitiesResponse = client.getIdentitiesByPublicKeyHashes(listOf(key.pubKeyHash))
        assertEquals(1, identitiesResponse.identities.size)
        assertEquals(0, identitiesResponse.identities[0].size)

        val identityId = client.getIdentityIdByFirstPublicKey(key.pubKeyHash)
        assertEquals(null, identityId)

        val identityIdListResponse = client.getIdentityIdsByPublicKeyHashes(listOf(key.pubKeyHash))
        assertEquals(1, identityIdListResponse.identityIds.size)
        assertEquals(0, identityIdListResponse.identityIds[0].size)
    }

    @Test
    fun getTransationTest() {
        val txid = "7609edb70ffe29ccad3054858abab0379965878bcafe18cf76dd80a6ccccf63d"
        val client = DapiClient(masternodeList)

        val result = client.getTransaction(txid)

        println(result)
    }
}
