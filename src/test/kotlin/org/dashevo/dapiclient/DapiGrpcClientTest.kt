package org.dashevo.dapiclient

import com.hashengineering.crypto.X11
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.params.EvoNetParams
import org.bitcoinj.params.MobileDevNetParams
import org.bitcoinj.params.PalinkaDevNetParams
import org.dashevo.dapiclient.model.DocumentQuery
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
        val client = DapiClient("19.233.82.208")
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
        val client = DapiClient(MobileDevNetParams.MASTERNODES[1])
        try {
            val status = client.getStatus()
            println(status)
        } finally {

        }
    }

    @Test
    fun getBlockTest() {
        val nodes = listOf("1.1.1.1", "2.2.2.2", MobileDevNetParams.MASTERNODES[1])
        val client = DapiClient(nodes)
        try {
            //devnet-mobile, devnet genesis block
            val block1 = "040000002e3df23eec5cd6a86edd509539028e2c3a3dc05315eb28f2baa43218ca080000b3a56d65316ffdb006163240a4380e94a4c2d8c0f0b3b2c1ddc486fae15ed065ba968054ffff7f20040000000101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff11510f6465766e65742d6d6f62696c652d32ffffffff0100f2052a01000000016a00000000"
            val block1Hash = "14a0ccc3b747208c4d765cd0a583a1701287fa67e2d85cd30c3b6e0578ffe8ee"

            // request the block from the height
            val blockFromHeight = client.getBlockByHeight(1)
            assertEquals(block1, blockFromHeight!!.toHexString())

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
        val contractId = "FiBkhut4LFPMJqDWbZrxVeT6Mr6LsH3mTNTSSHJY2ape"
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

    /*
    {
      "protocolVersion": 0,
      "type": 2,
      "actions": [
        1
      ],
      "documents": [
        {
          "$type": "note",
          "$contractId": "EzLBmQdQXYMaoeXWNaegK18iaaCDShitN3s14US3DunM",
          "$userId": "At44pvrZXLwjbJp415E2kjav49goGosRF3SB1WW1QJoG",
          "$entropy": "ydQUKu7QxqPxt4tytY7dtKM7uKPGzWG9Az",
          "$rev": 1,
          "message": "Tutorial Test @ Thu, 26 Mar 2020 20:19:49 GMT"
        }
      ],
      "signaturePublicKeyId": 1,
      "signature": "IFue3isoXSuYd0Ky8LvYjOMExwq69XaXPvi+IE+YT0sSD6N22P75xWZNFqO8RkZRqtmO7+EwyMX7NVETcD2HTmw=",
    }
     */
    @Test
    fun getDocumentsTest() {

        val masternodeList = EvoNetParams.MASTERNODES
        val client = DapiClient(masternodeList.toList())
        val contractId = "FiBkhut4LFPMJqDWbZrxVeT6Mr6LsH3mTNTSSHJY2ape"
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
        val id = "G3H7uJQHSC5NXqifnX1wqE6KB4PLTEBD5Q9dKQ3Woz38"
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