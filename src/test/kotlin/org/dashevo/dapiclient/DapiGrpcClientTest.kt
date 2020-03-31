package org.dashevo.dapiclient

import com.hashengineering.crypto.X11
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.evolution.CreditFundingTransaction
import org.bitcoinj.params.EvoNetParams
import org.dashevo.dapiclient.model.DocumentQuery
import org.dashevo.dpp.contract.Contract
import org.dashevo.dpp.document.Document
import org.dashevo.dpp.identity.Identity
import org.dashevo.dpp.toBase64
import org.dashevo.dpp.toHexString
import org.dashevo.dpp.util.Cbor
import org.dashj.bls.Utils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DapiGrpcClientTest {

    @Test
    fun getStatusOfInvalidNodeTest() {
        val client = DapiClient("19.233.82.208")
        try {
            val status = client.getStatus()
            fail<Nothing>("This test should throw and exception")
        } catch (e: StatusRuntimeException) {
            if(e.status.code != Status.UNAVAILABLE.code)
                fail<Nothing>("Invalid node test failed with a differnet error")
        } finally {
            client.shutdown()
        }
    }

    @Test
    fun getStatusTest() {
        val client = DapiClient("18.237.82.208")
        try {
            val status = client.getStatus()
            println(status)
        } finally {
            client.shutdown()
        }
    }

    @Test
    fun getBlockTest() {
        val client = DapiClient(SingleMasternode("18.237.82.208"))
        try {
            //devnet-mobile
            val block1 = "040000002e3df23eec5cd6a86edd509539028e2c3a3dc05315eb28f2baa43218ca08000073c0af0969b638432ca0744be69fdcf419e476c59ee08368002df63a28f6c0bbba968054ffff7f20000000000101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0f510d6465766e65742d6d6f62696c65ffffffff0100f2052a01000000016a00000000"
            val block1Hash = "674f4f0bcd708edd9fafaf7236db7971a43c2497f335e57582f1eaca5cb48f09"

            // request the block from the height
            val blockFromHeight = client.getBlockByHeight(1)
            assertEquals(block1, blockFromHeight!!.toHexString())

            // hash the block header and compare to the actual value
            val hash = Sha256Hash.wrapReversed(X11.x11Digest(blockFromHeight.take(80).toByteArray()))
            assertEquals("674f4f0bcd708edd9fafaf7236db7971a43c2497f335e57582f1eaca5cb48f09", hash.toString())

            // request the block from the hash and compare to the block obtained from the height
            val blockFromHash = client.getBlockByHash(block1Hash)
            assertEquals(blockFromHeight.toHexString(), blockFromHash!!.toHexString())

        } finally {
            client.shutdown()
        }
    }

    //77w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3

    @Test
    fun getContractTest() {

        val masternodeList = EvoNetParams.MASTERNODES
        val client = DapiClient(SingleMasternode(masternodeList[0]))
        val contractId = "77w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3"
        try {
            //devnet-evonet
            val contractBytes = client.getDataContract(contractId)
            println(contractBytes!!.toByteArray().toHexString())
            println(contractBytes!!.toByteArray().toBase64())

            val contract = Contract(Cbor.decode(contractBytes!!.toByteArray()))
            println(contract)
        } finally {
            client.shutdown()
        }
    }

    @Test
    fun getContractTest2() {

        val masternodeList = EvoNetParams.MASTERNODES
        val client = DapiClient(SingleMasternode(masternodeList[0]), false)
        val contractId = "77w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3"
        try {
            //devnet-evonet
            val contractBytes = client.getDataContract(contractId)
            println(contractBytes!!.toByteArray().toHexString())
            println(contractBytes!!.toByteArray().toBase64())
            val contractBytes2 = client.getDataContract(contractId)


            val contract = Contract(Cbor.decode(contractBytes!!.toByteArray()))
            println(contract)
        } finally {
            client.shutdown()
        }
    }

    @Test
    fun getContractTest3() {

        val masternodeList = EvoNetParams.MASTERNODES
        val client = DapiClient(SingleMasternode(masternodeList[0]), true)
        val contractId = "77w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3"
        try {
            //devnet-evonet
            val contractBytes = client.getDataContract(contractId)
            println(contractBytes!!.toByteArray().toHexString())
            println(contractBytes!!.toByteArray().toBase64())
            val contractBytes2 = client.getDataContract(contractId)


            val contract = Contract(Cbor.decode(contractBytes!!.toByteArray()))
            println(contract)
        } finally {
            client.shutdown()
        }
    }

    @Test
    fun getNonExistantContract() {

        val masternodeList = EvoNetParams.MASTERNODES
        val client = DapiClient(SingleMasternode(masternodeList[0]), true)
        val contractId = "88w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3"
        try {
            //devnet-evonet
            val contractBytes = client.getDataContract(contractId)
            assertTrue(contractBytes == null)
        } finally {
            client.shutdown()
        }
    }

    @Test
    fun queryInvalidNode() {
        val client = DapiClient(SingleMasternode("111.111.111.111"), true)
        val contractId = "88w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3"
        try {
            val contractBytes = client.getDataContract(contractId)
            fail<Nothing>("The node queried should not exist")
        } catch(e: StatusRuntimeException) {
            assertTrue(e.status.code == Status.UNAVAILABLE.code)
        } finally {
            client.shutdown()
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
        val client = DapiClient(SingleMasternode(masternodeList[0]))
        val contractId = "77w8Xqn25HwJhjodrHW133aXhjuTsTv9ozQaYpSHACE3"//""EzLBmQdQXYMaoeXWNaegK18iaaCDShitN3s14US3DunM"
        try {
            //devnet-evonet
            val where2 = listOf(
                    listOf("normalizedParentDomainName", "==", "dash").toMutableList(),
                    listOf("normalizedLabel", "startsWith", "a").toMutableList()
            ).toMutableList()

            val empty = listOf<String>().toMutableList()
            //listOf("message", "startsWith", "Tut").toMutableList()).toMutableList()
            val documents = client.getDocuments(contractId, "domain", DocumentQuery(where2, null, 100, 0, 0))
            println(documents!![0].toHexString())

            val document = Document(Cbor.decode(documents!![0]))

            val docs = ArrayList<Document>(documents.size)
            for (d in documents) {
                docs.add(Document(Cbor.decode(d)))
            }

            println(document)
        } finally {
            client.shutdown()
        }
    }
    /*
    CreditFundingTransaction{78b7f924ce99982d7bc95160984e38a2b17e88ba98b5583d4320116565c2763d
      type TRANSACTION_NORMAL(0)
    purpose: UNKNOWN
       in   PUSHDATA(72)[3045022100fc7cab994fb62bce2e286124d696cdd09120ac8ae94e4598977f1a27a582f747022074da17c595b531ce81b70d4116425a6df9b2a71f958f399dcabab9f205b2ae9e01] PUSHDATA(33)[0326e680733eefbf271cd20fddf40e75a89923b1cf39a6162baf770de040efb718]
            unconnected  outpoint:4953fde90ca2b118755d144ed1fa86cef5a93106a1efd317c895f37506935e74:0
       out  DUP HASH160 PUSHDATA(20)[7b560e12927197cfc4267f752280910a09db8fdb] EQUALVERIFY CHECKSIG  0.19959776 DASH
            P2PKH addr:yXZb416KVDkaMG8AuqpPRnGkftPWyyJR4b
       out  RETURN PUSHDATA(20)[6d22ab738e8b321738b382e1a10f4d0c50c905e9]  0.0004 DASH
            CREDITBURN addr:yWGW31QSArqjncjmn74AFaay7SCrP1oYUa
    }

      DeterministicKey{pub HEX=027874912e5d8e99cb129d4d0dc7c8285343c181c4c8272f89752c65b68ceb2c94,
        priv HEX=0c82adc6e085cfc27c6620b1fa8eb8ea630bccaf47b459fb128f7c1827994e8d,
        priv WIF=cN128hmiUpcpEX5AceYMyu9LDsnsqhqSoSufVU1HRBha1jebwhBx, isEncrypted=false, isPubKeyOnly=false}
        addr:yWGW31QSArqjncjmn74AFaay7SCrP1oYUa  hash160:6d22ab738e8b321738b382e1a10f4d0c50c905e9  (M/9H/1H/12H/0, external)

     */

    @Test
    fun registerIdentityTest() {
        /*val publicKeyHex = "027874912e5d8e99cb129d4d0dc7c8285343c181c4c8272f89752c65b68ceb2c94"
        val privateKeyHex = "0c82adc6e085cfc27c6620b1fa8eb8ea630bccaf47b459fb128f7c1827994e8d"
        val key = ECKey(Utils.HEX.decode(privateKeyHex), Utils.HEX.decode(publicKeyHex))
        val identityPublicKey = IdentityPublicKey(1,
                IdentityPublicKey.TYPES.ECDSA_SECP256K1, key.pubKey.toBase64(), true)
        val keyList = java.util.ArrayList<IdentityPublicKey>()
        keyList.add(identityPublicKey)

        val st = IdentityCreateTransition(Identity.IdentityType.USER,
                TransactionOutPoint(EvoNetParams.get(),1,
                        Sha256Hash.wrapReversed(Utils.HEX.decode("78b7f924ce99982d7bc95160984e38a2b17e88ba98b5583d4320116565c2763d"))).toStringBase64(),
                        keyList, 0)

        st.sign(identityPublicKey, Utils.HEX.encode(key.privKeyBytes))

        val client = DapiClient(SingleMasternode(EvoNetParams.MASTERNODES[1]), true)
        client.applyStateTransition(st)
        client.shutdown()*/
    }

    @Test
    fun getIdentityTest() {
        val tx = "0100000001745e930675f395c817d3efa10631a9f5ce86fad14e145d7518b1a20ce9fd5349000000006b483045022100fc7cab994fb62bce2e286124d696cdd09120ac8ae94e4598977f1a27a582f747022074da17c595b531ce81b70d4116425a6df9b2a71f958f399dcabab9f205b2ae9e01210326e680733eefbf271cd20fddf40e75a89923b1cf39a6162baf770de040efb718ffffffff02e08f3001000000001976a9147b560e12927197cfc4267f752280910a09db8fdb88ac409c000000000000166a146d22ab738e8b321738b382e1a10f4d0c50c905e900000000"

        val cftx = CreditFundingTransaction(EvoNetParams.get(), Utils.HEX.decode(tx))

        val client = DapiClient(SingleMasternode(EvoNetParams.MASTERNODES[1]), true)
        val identity = client.getIdentity(cftx.creditBurnIdentityIdentifier.toStringBase58())
        val i = Identity(Cbor.decode(identity!!.toByteArray()))

        val identity2 = client.getIdentity(cftx.creditBurnIdentityIdentifier.toStringBase58())

        val txData = client.getTransaction(cftx.txId.reversedBytes.toHexString())

        client.shutdown()
    }
}