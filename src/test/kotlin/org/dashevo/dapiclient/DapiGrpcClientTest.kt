package org.dashevo.dapiclient

import com.google.common.base.Stopwatch
import com.hashengineering.crypto.X11
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.bitcoinj.core.*
import org.bitcoinj.params.*
import org.dashevo.dapiclient.model.DocumentQuery
import org.dashevo.dapiclient.provider.DAPIAddress
import org.dashevo.dapiclient.provider.ListDAPIAddressProvider
import org.dashevo.dpp.contract.ContractFactory
import org.dashevo.dpp.document.Document
import org.dashevo.dpp.identity.IdentityFactory
import org.dashevo.dpp.toHexString
import org.dashevo.dpp.util.Cbor
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class DapiGrpcClientTest {

    val PARAMS = TestNet3Params.get()
    val CONTEXT = Context.getOrCreate(PARAMS)
    val masternodeList = PARAMS.defaultMasternodeList.toList()
    val dpnsContractId = Base58.decode("76wgB8KBxLGhtEzn4Hp5zgheyzzpHYvfcWGLs69B2ahq") //DPNS contract
    val dashPayContractId = Base58.decode("6wfobip5Mfn6NNGK9JTQ5eHtZozpkNx4aZUsnCxkfgj5")
    val identityIdString = "4yaJaaeUU9xG6sonkCHZkcZkhcXGqwf5TcNLw5Nh5LJ4"
    val stateRepository = StateRepositoryMock()


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
                if (cause.status.code != Status.UNAVAILABLE.code && cause.status.code != Status.DEADLINE_EXCEEDED.code)
                    fail<Nothing>("Invalid node test failed with a different error")
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
            //devnet-mobile, devnet genesis block
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
            val contractBytes = client.getDataContract(dpnsContractId)

            val contract = ContractFactory(stateRepository).createFromBuffer(contractBytes!!.toByteArray())
            val jsonDpnsFile = File("src/test/resources/dpns-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = ContractFactory(stateRepository).createFromObject(rawContract)

            assertEquals(dpnsContract.toJSON(), contract.toJSON())
        } finally {

        }
    }

    @Test
    fun getDashPayContractTest() {

        val client = DapiClient(masternodeList)
        try {
            val contractBytes = client.getDataContract(dashPayContractId)

            val contract = ContractFactory(stateRepository).createFromBuffer(contractBytes!!.toByteArray())
            val jsonDpnsFile = File("src/test/resources/dashpay-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = ContractFactory(stateRepository).createFromObject(rawContract)

            assertEquals(dpnsContract.toJSON(), contract.toJSON())
        } finally {

        }
    }

    @Test
    fun getNonExistantContract() {

        val client = DapiClient(masternodeList.toList())
        val contractId = Base58.decode("88w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3")
        try {
            val contractBytes = client.getDataContract(contractId)
            assertTrue(contractBytes == null)
        } finally {

        }
    }

    @Test
    fun getDocumentsTest() {

        val client = DapiClient(masternodeList.toList())
        try {
            //devnet-evonet
            val query = DocumentQuery.Builder()
                    .where(listOf("normalizedParentDomainName", "==", "dash").toMutableList())
                    //.where(listOf("normalizedLabel", "startsWith", "test").toMutableList())
                    .build()
            val documents = client.getDocuments(dpnsContractId, "domain", query)
            println(documents!![0].toHexString())

            val jsonDpnsFile = File("src/test/resources/dpns-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = ContractFactory(stateRepository).createFromObject(rawContract)

            val document = Document(Cbor.decode(documents[0]), dpnsContract)

            val docs = ArrayList<Document>(documents.size)
            for (d in documents) {
                docs.add(Document(Cbor.decode(d), dpnsContract))
            }

            println(document.toJSON())
        } finally {

        }
    }

    @Test
    fun getIdentityTest() {
        val id = Base58.decode(identityIdString)
        val badId = Base58.decode(identityIdString.replace("4", "3"))
        val client = DapiClient(masternodeList)
        val identityBytes = client.getIdentity(id)
        val badIdentityBytes = client.getIdentity(badId)
        assertEquals(null, badIdentityBytes)
        val identity = IdentityFactory(stateRepository).createFromBuffer(identityBytes!!.toByteArray())
        println(JSONObject(identity.toJSON()).toString(2))


        val pubKeyHash = ECKey.fromPublicOnly(identity.getPublicKeyById(0)!!.data).pubKeyHash
        val identityByPublicKeyBytes = client.getIdentityByFirstPublicKey(pubKeyHash)
        val identitiesByPublicKeyHashes = client.getIdentitiesByPublicKeyHashes(listOf(pubKeyHash))
        val identityByPublicKey = IdentityFactory(stateRepository).createFromBuffer(identityByPublicKeyBytes!!.toByteArray())
        val identityByPublicKeyHashes = IdentityFactory(stateRepository).createFromBuffer(identitiesByPublicKeyHashes!![0].toByteArray())
        val identityIdByPublicKey = client.getIdentityIdByFirstPublicKey(pubKeyHash)

        val identityIdsByPublicKey = client.getIdentityIdsByPublicKeyHashes(listOf(pubKeyHash))

        assertEquals(identityIdString, identityByPublicKey.id.toString())
        assertEquals(identityIdString, identityByPublicKeyHashes!!.id.toString())
        assertArrayEquals(id, identityIdByPublicKey!!.toByteArray())
        assertArrayEquals(id, identityIdsByPublicKey!![0].toByteArray())
    }

    @Test
    fun getIdentityFromBadPubKeyBytes() {
        val key = ECKey()
        val client = DapiClient(masternodeList)

        val identity = client.getIdentityByFirstPublicKey(key.pubKeyHash)
        assertEquals(null, identity)

        val identities = client.getIdentitiesByPublicKeyHashes(listOf(key.pubKeyHash))
        assertEquals(null, identities)

        val identityId = client.getIdentityIdByFirstPublicKey(key.pubKeyHash)
        assertEquals(null, identityId)

        val identityIdList = client.getIdentityIdsByPublicKeyHashes(listOf(key.pubKeyHash))
        assertEquals(null, identityIdList)
    }
}