package org.dashevo.dapiclient

import com.hashengineering.crypto.X11
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.params.EvoNetParams
import org.bitcoinj.params.MobileDevNetParams
import org.bitcoinj.params.PalinkaDevNetParams
import org.dashevo.dapiclient.model.DocumentQuery
import org.dashevo.dapiclient.provider.DAPIAddress
import org.dashevo.dapiclient.provider.ListDAPIAddressProvider
import org.dashevo.dpp.contract.ContractFactory
import org.dashevo.dpp.document.Document
import org.dashevo.dpp.identity.IdentityFactory
import org.dashevo.dpp.toHexString
import org.dashevo.dpp.util.Cbor
import org.dashevo.dpp.util.HashUtils
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class DapiGrpcClientTest {

    @Test
    fun getStatusOfInvalidNodeTest() {
        val list = ListDAPIAddressProvider(listOf("127.0.0.1").map { DAPIAddress(it) }, 0)
        val client = DapiClient(list)
        try {
            client.getStatus()
            fail<Nothing>("The node queried should not exist")
        } catch (e: NoAvailableAddressesForRetryException) {
            val cause = e.cause as StatusRuntimeException
            if (cause.status.code != Status.UNAVAILABLE.code)
                fail<Nothing>("Invalid node test failed with a different error")
        }
    }

    @Test
    fun getStatusTest() {
        val client = DapiClient(EvoNetParams.MASTERNODES.toList())
        try {
            val status = client.getStatus()
            println(status)
        } finally {

        }
    }

    @Test
    fun getBlockTest() {
        val nodes = listOf("127.0.0.1", EvoNetParams.MASTERNODES[1])
        val client = DapiClient(nodes)
        try {
            val params = EvoNetParams.get()
            //devnet-mobile, devnet genesis block
            val block1 = params.devNetGenesisBlock
            val block1data = block1.bitcoinSerialize().toHexString()
            val block1Hash = block1.hashAsString

            // request the block from the height
            val blockFromHeight = client.getBlockByHeight(1)
            assertEquals(block1data, blockFromHeight!!.toHexString())

            // hash the block header and compare to the actual value
            val hash = Sha256Hash.wrapReversed(X11.x11Digest(blockFromHeight.take(80).toByteArray()))
            assertEquals(block1Hash, hash.toString())

            // request the block from the hash and compare to the block obtained from the height
            val blockFromHash = client.getBlockByHash(block1Hash)
            assertEquals(blockFromHeight.toHexString(), blockFromHash!!.toHexString())

        } finally {

        }
    }


    @Test
    fun getDPNSContractTest() {

        val client = DapiClient(EvoNetParams.MASTERNODES.toList())
        val contractId = "566vcJkmebVCAb2Dkj2yVMSgGFcsshupnQqtsz1RFbcy"
        try {
            val contractBytes = client.getDataContract(contractId)

            val contract = ContractFactory().createFromSerialized(contractBytes!!.toByteArray())

            val jsonDpnsFile = File("src/test/resources/dpns-contract.json").readText()
            val jsonDpns = JSONObject(jsonDpnsFile)
            val rawContract = jsonDpns.toMap()
            val dpnsContract = ContractFactory().createFromObject(rawContract)

            assertEquals(dpnsContract.toJSON(), contract.toJSON())
        } finally {

        }
    }

    @Test
    fun getNonExistantContract() {

        val masternodeList = EvoNetParams.MASTERNODES
        val client = DapiClient(masternodeList.toList(), true)
        val contractId = "88w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3"
        try {
            val contractBytes = client.getDataContract(contractId)
            assertTrue(contractBytes == null)
        } finally {

        }
    }

    @Test
    fun getDocumentsTest() {

        val masternodeList = EvoNetParams.MASTERNODES
        val client = DapiClient(masternodeList.toList())
        val contractId = "566vcJkmebVCAb2Dkj2yVMSgGFcsshupnQqtsz1RFbcy"
        try {
            //devnet-evonet
            val query = DocumentQuery.Builder()
                    .where(listOf("normalizedParentDomainName", "==", "dash").toMutableList())
                    .where(listOf("normalizedLabel", "startsWith", "test").toMutableList())
                    .build()
            val documents = client.getDocuments(contractId, "domain", query)
            println(documents!![0].toHexString())

            val document = Document(Cbor.decode(documents[0]))

            val docs = ArrayList<Document>(documents.size)
            for (d in documents) {
                docs.add(Document(Cbor.decode(d)))
            }

            println(document.toJSON())
        } finally {

        }
    }

    @Test
    fun getIdentityTest() {
        val id = Base58.encode(HashUtils.fromHex("a2615875e74fc77c153d6dd0561dd9996a7fc8ca9f04d98addc2f9a7778ac702"))
        val badId = "G3H7uJQHSC5NXqifnX1wqE6KB4PLTEBD5Q9dKQ3Woz39"
        val client = DapiClient(PalinkaDevNetParams.get().defaultMasternodeList.toList(), false)
        val identityBytes = client.getIdentity(id)
        val badIdentityBytes = client.getIdentity(badId)
        assertEquals(null, badIdentityBytes)
        val identity = IdentityFactory().createFromSerialized(identityBytes!!.toByteArray())

        val pubKeyHash = ECKey.fromPublicOnly(HashUtils.byteArrayFromString(identity.getPublicKeyById(0)!!.data)).pubKeyHash
        val identityByPublicKeyBytes = client.getIdentityByFirstPublicKey(pubKeyHash)
        val identityByPublicKey = IdentityFactory().createFromSerialized(identityByPublicKeyBytes!!.toByteArray())
        val identityIdByPublicKey = client.getIdentityIdByFirstPublicKey(pubKeyHash)

        assertEquals(id, identityByPublicKey.id)
        assertEquals(id, identityIdByPublicKey)
    }
}