/**
 * Copyright (c) 2021-present, Dash Core Team
 *
 * This source code is licensed under the MIT license found in the
 * COPYING file in the root directory of this source tree.
 */

package org.dashj.platform.dapiclient

import com.google.protobuf.ByteString
import org.bitcoinj.core.Context
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.BLSPublicKey
import org.bitcoinj.params.KrupnikDevNetParams
import org.bitcoinj.quorums.LLMQParameters
import org.bitcoinj.quorums.Quorum
import org.dash.platform.dapi.v0.PlatformOuterClass
// import org.dashj.bls.BLS
import org.dashj.bls.PublicKey
import org.dashj.merk.ByteArrayKey
import org.dashj.merk.MerkVerifyProof
import org.dashj.merk.blake3
import org.dashj.platform.dapiclient.errors.NotFoundException
import org.dashj.platform.dapiclient.model.DocumentQuery
import org.dashj.platform.dapiclient.model.Proof
import org.dashj.platform.dapiclient.model.ResponseMetadata
import org.dashj.platform.dapiclient.proofs.Indices
import org.dashj.platform.dapiclient.proofs.MerkleProof
import org.dashj.platform.dapiclient.proofs.MerkleTree
import org.dashj.platform.dapiclient.proofs.ProofVerifier
import org.dashj.platform.dpp.DashPlatformProtocol
import org.dashj.platform.dpp.identifier.Identifier
import org.dashj.platform.dpp.identity.IdentityFactory
import org.dashj.platform.dpp.toBase58
import org.dashj.platform.dpp.toByteArray
import org.dashj.platform.dpp.toHex
import org.dashj.platform.dpp.util.Converters
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ProofTest {

    init {
        // TODO: disable these for now since GitHub Actions will need a build step for both of these
        // libraries.  We cannot use the binaries included in src/main/jniLibs
        // MerkVerifyProof.init()
        // BLS.Init()
    }

    val PARAMS = KrupnikDevNetParams.get()
    val CONTEXT = Context.getOrCreate(PARAMS)
    val masternodeList = PARAMS.defaultMasternodeList.toList()
    val dpnsContractId = SystemIds.dpnsDataContractId // DPNS contract
    val dashPayContractId = SystemIds.dashpayDataContractId
    val identityId = SystemIds.dashpayOwnerId
    val badDpnsContractId = Identifier.from("5BrYpaW5s26UWoBk9zEAYWxJANX7LFinmToprWo3VwgS") // DPNS contract
    val badDashPayContractId = Identifier.from("8FmdUoXZJijvARgA3Vcg73ThYp5P4AaLis1WpXp9VGg1")
    val badIdentityId = Identifier.from("GrdbRMnZ5pPiFWuzPR62goRVj6sxpqvLKMT87ZmuZPyr")

    val hash160 = Utils.sha256hash160(Converters.fromHex("03e32b81d4b8c34c170f8db115ef4609a41e08ea0e9d153f2059d993ab240af5d0")).toHex()
    val hash160a = Utils.sha256hash160(Converters.fromHex("0330fa594258df21f28bb5f698b88b1be008561180d458c176213f5be6be823c4d")).toHex()

    val publicKeyHash = Converters.fromHex("1e35a0e326f75f04b082fc058ca413d41c667261")
    val publicKeyHashes = listOf(publicKeyHash, Converters.fromHex("c3c85d2210e866b5ebf0318140a7005e9c8d1211"))

    val badPublicKeyHash = Converters.fromHex("ea396d727565f94d26f85e7f8a4fe5418f97d7cb")
    val badPublicKeyHashes = listOf(badPublicKeyHash, Converters.fromHex("bad3374c8aa0059809d677bcb44c86d4e7746bb9"))

    val goodBadPublicKeyHashes = listOf(publicKeyHash, badPublicKeyHash)

    val stateRepository = StateRepositoryMock()
    val dpp = DashPlatformProtocol(stateRepository, PARAMS)
    val client = DapiClient(masternodeList.toList(), dpp)

    @Test
    fun getIdentityWithProof() {
        try {
            val identityBytes = client.getIdentity(identityId.toBuffer(), false).identity
            val identity = dpp.identity.createFromBuffer(identityBytes)
            assertEquals(identityId, identity.id)
            println("identity: ${identityBytes.toHex()}")

            val identityBytesWithProof = client.getIdentity(identityId.toBuffer(), true).identity
            assertArrayEquals(identityBytes, identityBytesWithProof)
            println("identity: ${identityBytesWithProof.toHex()}")

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
        try {
            val query = DocumentQuery.Builder()
                .where("normalizedParentDomainName", "==", "dash")
                .where("normalizedLabel", "startsWith", "hash")
                .orderBy("normalizedLabel")
                .build()
            val documentsResponse = client.getDocuments(dpnsContractId.toBuffer(), "domain", query, false)
            println("documents: ${documentsResponse.documents.size}")

            val documentsWithProof = client.getDocuments(dpnsContractId.toBuffer(), "domain", query, true)
            println("documents: ${documentsWithProof.documents.size}")

            val badQuery = DocumentQuery.Builder()
                .where(listOf("normalizedParentDomainName", "==", "dash").toMutableList())
                .where(listOf("normalizedLabel", "startsWith", "8z9y7z").toMutableList())
                .orderBy("normalizedLabel", true)
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
        try {
            // get the dpns contract without a proof
            val contractBytes = client.getDataContract(dpnsContractId.toBuffer(), false).dataContract
            val dataContract = dpp.dataContract.createFromBuffer(contractBytes)
            assertEquals(dpnsContractId, dataContract.id)
            println("contract: ${contractBytes.toHex()}")

            val constractBytesWithProof = client.getDataContract(dpnsContractId.toBuffer(), true).dataContract
            assertArrayEquals(contractBytes, constractBytesWithProof)
            println("contract: ${constractBytesWithProof.toHex()}")

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
        try {
            val identityBytes = client.getIdentityByFirstPublicKey(publicKeyHash, false)!!
            val identity = dpp.identity.createFromBuffer(identityBytes)
            assertEquals(SystemIds.dpnsOwnerId, identity.id)
            println("identity id: ${identityBytes.toHex()}")

            val identityWithProof = client.getIdentityByFirstPublicKey(publicKeyHash, true)!!
            assertArrayEquals(identityBytes, identityWithProof)
            println("identity id: ${identityWithProof.toHex()}")

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
        try {
            val identityIds = client.getIdentityIdByFirstPublicKey(publicKeyHash, false)!!
            println("identity id: ${identityIds.toBase58()}")

            val identityIdsWithProof = client.getIdentityIdByFirstPublicKey(publicKeyHash, true)!!
            println("identity id: ${identityIdsWithProof.toBase58()}")

            val badIdentityIds = client.getIdentityIdByFirstPublicKey(badPublicKeyHash, false)
            println("identity id: ${badIdentityIds?.toHex()}")

            val badIdentityIdsWithProof = client.getIdentityIdByFirstPublicKey(badPublicKeyHash, true)
            println("identity id: ${badIdentityIdsWithProof?.toHex()}")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getIdentityIdsWithProof() {
        try {
            val response = client.getIdentityIdsByPublicKeyHashes(publicKeyHashes, false)
            assertEquals(SystemIds.dpnsOwnerId, response.identityIds[0].toBase58())
            println("identity ids: ${response.identityIds.map { it.toBase58()}}")

            val responseWithProof = client.getIdentityIdsByPublicKeyHashes(publicKeyHashes, true)
            assertEquals(listOf(SystemIds.dpnsOwnerId, SystemIds.dashpayOwnerId), responseWithProof.identityIds.map { Identifier.from(it) })
            println("identity ids: ${responseWithProof.identityIds.map { it.toBase58()}}")

            val badResponse = client.getIdentityIdsByPublicKeyHashes(badPublicKeyHashes, false)
            println("identity ids: ${badResponse.identityIds.size}")

            val badResponseWithProof = client.getIdentityIdsByPublicKeyHashes(badPublicKeyHashes, true)
            println("identity ids: ${badResponseWithProof.identityIds.size}")

            val goodBadResponse = client.getIdentityIdsByPublicKeyHashes(goodBadPublicKeyHashes, false)
            println("identity ids: ${goodBadResponse.identityIds.size}")

            val goodBadResponseWithProof = client.getIdentityIdsByPublicKeyHashes(goodBadPublicKeyHashes, true)
            println("identity ids: ${goodBadResponseWithProof.identityIds.size}")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    @Test
    fun getIdentitiesWithProof() {
        try {
            val response = client.getIdentitiesByPublicKeyHashes(publicKeyHashes, false)
            println("identities: ${response.identities.map { it.toHex()}}")

            val responseWithProof = client.getIdentitiesByPublicKeyHashes(publicKeyHashes, true)
            println("identities: ${responseWithProof.identities.map { it.toHex()}}")

            val badResponse = client.getIdentitiesByPublicKeyHashes(badPublicKeyHashes, false)
            println("identities: ${badResponse.identities.map { it.toHex()}}}")

            val badResponseWithProof = client.getIdentitiesByPublicKeyHashes(badPublicKeyHashes, true)
            println("identities: ${badResponseWithProof.identities.map { it.toHex()}}}")

            val goodBadResponse = client.getIdentitiesByPublicKeyHashes(goodBadPublicKeyHashes, false)
            println("identities: ${goodBadResponse.identities.map { it.toHex()}}}")

            val goodBadResponseWithProof = client.getIdentitiesByPublicKeyHashes(goodBadPublicKeyHashes, true)
            println("identities: ${goodBadResponseWithProof.identities.map { it.toHex()}}}")
        } catch (e: Exception) {
            fail("exception", e)
        }
    }

    // @Test
    fun testNonInclusionProof() {
        val proofData = Converters.fromHex("0a60bfef7d172b666943c33fae47b614259412f52435edd99bbf933144411c3aeab49b901c60efbd5040ab1122197418963b88d06dc440b88e02efca9292f0f0f275072907f22609678cb56cacace7bbd6ca9d7b6db1effd1d674a83cab37d9d40ba12fa040af70401f5935375cf59fffdb6e1a952095920fc6c3be6e40ac4d544e54a1c04d72029ac02c97ff70a287f4d9741f5c54e5fc5e6a365043cdbedf623ae7d0e280a6a32b70b1001581ec666a851b0a6547f14bfcbd9d6b21f7f7fb944ef694dd00a28d9dc710ee302d6319f3e691f57474ccae8f14c482b856dc74559c1648b7f7640748f2de7f5a110032090c756d54d86613eee798c7c63901b3737103a4fff8e226ca0018bbd71073c21007e01000000a4626964582090c756d54d86613eee798c7c63901b3737103a4fff8e226ca0018bbd71073c216762616c616e636519117b687265766973696f6e006a7075626c69634b65797381a3626964006464617461582102ff7d43b945d51e15e2d18e6f726c8570eb48653729f57e606160075cc00a181564747970650011032097bbfe64471f69f36bc532b97c4cd807ceb0c9e14d6dfdc901c867218eb753ad007e01000000a4626964582097bbfe64471f69f36bc532b97c4cd807ceb0c9e14d6dfdc901c867218eb753ad6762616c616e6365190340687265766973696f6e006a7075626c69634b65797381a3626964006464617461582103901425c2e735100b33b1d86694846d3f19d6304c8036c04ddebf7f562dfe112264747970650010014278b5602a918a552629503a8389953f4846f1d8c70ed3d657e520aa3483562c11027f39f41ed72cdee1a84117a520a0e92ad615ad73884c9e752f5d27593d9f7eea1001114f06e497bcc272aa13d7c8e7b185283180a4dba5549331fa599d6f62ac139811021ff2e1078cea7c2c27daf05eed4a93d222105b894df3a39f0f2dbca7102ea9e31001be87efded4c0b8be1e396ae07aed3f332c13929a04745e531912dd21f18d256911111a20000000d5aaeab71f10c0a31433968840113801f489c0b2c93a8ce78da75e317f22608b7118182a6b5c1480ee0c9d4399138a2de5b883fe5fe71a41b190aacc70883514712f15ff8f08ab16925231fe72172508c9de9b8f7676d9b6ce5b229fb6611a0b05144efa22cb8b943d10ba94a56a1592220fe5c196193c571d1f6beeb11261")

        val metaData = Converters.fromHex("08db2210c9a803")

        val signatureLLMQHashData = Converters.fromHex("000000d5aaeab71f10c0a31433968840113801f489c0b2c93a8ce78da75e317f")
        val signatureData = Converters.fromHex("8b7118182a6b5c1480ee0c9d4399138a2de5b883fe5fe71a41b190aacc70883514712f15ff8f08ab16925231fe72172508c9de9b8f7676d9b6ce5b229fb6611a0b05144efa22cb8b943d10ba94a56a1592220fe5c196193c571d1f6beeb11261")
        val rootTreeProofData = Converters.fromHex("bfef7d172b666943c33fae47b614259412f52435edd99bbf933144411c3aeab49b901c60efbd5040ab1122197418963b88d06dc440b88e02efca9292f0f0f275072907f22609678cb56cacace7bbd6ca9d7b6db1effd1d674a83cab37d9d40ba")
        val identitiesProofData = Converters.fromHex("01f5935375cf59fffdb6e1a952095920fc6c3be6e40ac4d544e54a1c04d72029ac02c97ff70a287f4d9741f5c54e5fc5e6a365043cdbedf623ae7d0e280a6a32b70b1001581ec666a851b0a6547f14bfcbd9d6b21f7f7fb944ef694dd00a28d9dc710ee302d6319f3e691f57474ccae8f14c482b856dc74559c1648b7f7640748f2de7f5a110032090c756d54d86613eee798c7c63901b3737103a4fff8e226ca0018bbd71073c21007e01000000a4626964582090c756d54d86613eee798c7c63901b3737103a4fff8e226ca0018bbd71073c216762616c616e636519117b687265766973696f6e006a7075626c69634b65797381a3626964006464617461582102ff7d43b945d51e15e2d18e6f726c8570eb48653729f57e606160075cc00a181564747970650011032097bbfe64471f69f36bc532b97c4cd807ceb0c9e14d6dfdc901c867218eb753ad007e01000000a4626964582097bbfe64471f69f36bc532b97c4cd807ceb0c9e14d6dfdc901c867218eb753ad6762616c616e6365190340687265766973696f6e006a7075626c69634b65797381a3626964006464617461582103901425c2e735100b33b1d86694846d3f19d6304c8036c04ddebf7f562dfe112264747970650010014278b5602a918a552629503a8389953f4846f1d8c70ed3d657e520aa3483562c11027f39f41ed72cdee1a84117a520a0e92ad615ad73884c9e752f5d27593d9f7eea1001114f06e497bcc272aa13d7c8e7b185283180a4dba5549331fa599d6f62ac139811021ff2e1078cea7c2c27daf05eed4a93d222105b894df3a39f0f2dbca7102ea9e31001be87efded4c0b8be1e396ae07aed3f332c13929a04745e531912dd21f18d25691111")

        val proof = Proof(PlatformOuterClass.Proof.parseFrom(proofData))

        assertArrayEquals(proof.signature, signatureData, "Signature must match")
        assertArrayEquals(proof.signatureLlmqHash, signatureLLMQHashData, "Signature quorum must match")
        assertArrayEquals(proof.rootTreeProof, rootTreeProofData, "Root tree proof must match")
        assertArrayEquals(proof.storeTreeProofs.identitiesProof, identitiesProofData, "Identity tree proof must match")

        // Quorum()
        val quorumEntry = Quorum(
            PARAMS,
            LLMQParameters.fromType(LLMQParameters.LLMQType.LLMQ_DEVNET),
            Sha256Hash.wrapReversed(Converters.fromHex("7f315ea78de78c3ac9b2c089f40138114088963314a3c0101fb7eaaad5000000")),
            BLSPublicKey(PublicKey.FromBytes(Converters.fromHex("0a396fd00ac8f678a242c4b14004fe3402bdb9ada641e48e11ca6be3c87c5858b4cbc6014622d98df95b1a68b1bbd46c")))
        )

        val expectedStateHash = "c0607cbb713a37b4bb352493ea29e30ff301aaaa1951b31644467d695ff4994f".toByteArray()
        var identitiesPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null

        val rootElementsToProve = hashMapOf<Int, ByteArray>()
        if (proof.storeTreeProofs.identitiesProof.isNotEmpty()) {
            identitiesPair = MerkVerifyProof.extractProofWithHash(proof.storeTreeProofs.identitiesProof)
            rootElementsToProve[Indices.Identities.value] = identitiesPair.first
        }
        println("non-rootElementsToProve: ${rootElementsToProve.values.map { it.toHex() }}")

        val merkleProof = MerkleProof.fromBuffer(proof.rootTreeProof) {
            blake3(it)
        }
        val stateHash = merkleProof.calculateRoot(rootElementsToProve.keys.toList(), rootElementsToProve.values.toList(), 6)

        assertEquals(expectedStateHash.toHex(), stateHash.toHex())

        val responseMetaData = ResponseMetadata(metaData)
        val results = ProofVerifier.verifyAndExtractFromProof(proof, responseMetaData, quorumEntry, "identity")

        assertEquals(results.keys.size, 1)
    }

    // @Test
    fun testDocumentProof() {
        val rootTreeProofData = Converters.fromHex("9ad9dfb0bff451102a1cc1bb31b01961eacb3ba0cbb719bef5646961cfed7095fcdfe69e113d45895aff3e385b4704d6c0823a142fd612cd2acf85ae5599e985")
        val documentsProofData = Converters.fromHex("010e6597fb5777b54c2112476fd92cc34410bfd61bbe8c9c6ee79a9c141cec9a0c02bf7c4ce1e69c251a03bfd65ddc3cc0d86e33c6d04923b336185edccf6fd6d17a10028fb8b5a89ecf7328a178f1da5e9d661282baa0b394531e93c630335a5d852cfc03200c3973bf9e5517e687e3d7a30eef5dbb63b0399ac2dc4146533eacd6707d87ed019101000000ab6324696458200c3973bf9e5517e687e3d7a30eef5dbb63b0399ac2dc4146533eacd6707d87ed65247479706566646f6d61696e656c6162656c746639386365623631366137313765653636666431677265636f726473a17464617368556e697175654964656e74697479496458201d9f35f7769fbf19b8af883c61a86ceb6148d122864cbc711cf84a646970698e68246f776e6572496458201d9f35f7769fbf19b8af883c61a86ceb6148d122864cbc711cf84a646970698e69247265766973696f6e016c7072656f7264657253616c745820c6dff3f43533f7db07288206d0345534b04220aad9e365e2ff4f229f4420f7386e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c746639386365623631366137313765653636666431781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d656464617368110226f8f9a41eaaf7cb5989b2dc4854630164c31f1165564d8838162216b94c2c771001c0fde626739d4425aa60676cb10eee4b96eead14e1ac34341827371b72a341fc1111027c5f0fe0b67826fe211a5996577521edf97a9dc038363396a460cae2ca8963371001ae4a0d2eebe0c6cf4b59c27b644def624fd1d9bdb694628279746aced65cfbd402fd9c813110468eecb670e5bc6b305193f156329a828ef08cf3790a3c8bba0ef410032025071c8a421f0e2dfd3ad6662b06c93c329a0031ccf884e8f89ce18c97db0611017301000000ab63246964582025071c8a421f0e2dfd3ad6662b06c93c329a0031ccf884e8f89ce18c97db061165247479706566646f6d61696e656c6162656c656861736833677265636f726473a17464617368556e697175654964656e7469747949645820be93b13e4e35dbe3059203b27c517cb94b11821e8686254ce09a88da46ae9a5268246f776e657249645820be93b13e4e35dbe3059203b27c517cb94b11821e8686254ce09a88da46ae9a5269247265766973696f6e016c7072656f7264657253616c74582017d47f6566298f57bf72a1d49171cbd1e6404bb31deaaf29e3f1ac6a8f3b80526e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c656861736833781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d6564646173680265088f1f6f2d43894ae8376a427df399bcef6fa77284239016e4c7d18884ec371001f558d3fc7b849a5949d1362cfea0b31ef10934e508ba2929c824c300b18cfc331102a1c72d3b2c97f513175f0d7d8faa296d0aaf2140a062c0bb053ce519fb3f06e410010f8b3405a8bba408810e6e5779e3382ee7f5f85749df46099026b567f8f2757011111102b3e9fcbe3a940cdd9b0a2de890beab2076626ef66782fe49577f84e4a15b2f801001884af55d3769ba01a8144e1822917ecf8db172b55c543f2af7827e361373de6f02e1f875f804056f61d8ce4fcf5d91f747cf5684314dd120cd4157ede08c94628910016c4511d94fba6e166a604422430179d1ac45402d5c6eab1251cba5e621812b3f03205656ac0a0c1d4aaf32f047a3216f50cd9d459374a6e2d3e815d51561cbbb3a79017d01000000ab6324696458205656ac0a0c1d4aaf32f047a3216f50cd9d459374a6e2d3e815d51561cbbb3a7965247479706566646f6d61696e656c6162656c6a726567696f6e55736572677265636f726473a17464617368556e697175654964656e74697479496458206921062f7a53abe95ebbd6a42e755a29ec19a87cc03ba188150a7d51fef47e0a68246f776e6572496458206921062f7a53abe95ebbd6a42e755a29ec19a87cc03ba188150a7d51fef47e0a69247265766973696f6e016c7072656f7264657253616c7458206a5dbf3820345f4397e681bad11b6842ccf16290ee2a7cf82651a81f994d864b6e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c6a726567696f6e75736572781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d65646461736810011265824fbf2c310891364d6a5eeac719115dfd9e245b1251d67ced801216227f111102cd406bb9ba1c5898c00f18a7423d390277a48555bed0ea49f148ed8b2c792d841001ab0f9c4f9e179bb1721241dfd633352e5ee162c231f08fd5ff010dc409a5726802584d3bec75543bec8d577a41fc728f9b32be96f98cb16fbf41ae1d18a6347f841001696c8a3a9c8a1f22c50810be78777f73206d35f5bd6e66848c0c1e8bd9b24fb203206e2502976df84598c68e3c131739cea25b5b50e053c042167bab75b1e6a2e88f018701000000ab6324696458206e2502976df84598c68e3c131739cea25b5b50e053c042167bab75b1e6a2e88f65247479706566646f6d61696e656c6162656c6f7363686e617070732d757365722d31677265636f726473a17464617368556e697175654964656e7469747949645820d9110f3fcc91137bb9784dfb2bb30a259d2696f7870b7041dd011a1e26d3399568246f776e657249645820d9110f3fcc91137bb9784dfb2bb30a259d2696f7870b7041dd011a1e26d3399569247265766973696f6e016c7072656f7264657253616c745820ab6a7ff9a8e8fa4da217be33001e58a54ca2c47246b3bd47f1d689ade89ac4b56e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c6f7363686e617070732d757365722d31781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d65646461736810011d5d5ade5f36bba078f91057bd7a4bedbee40dbaec487ff381ccf40f9e8a0080111102927f774e11d6b2c74184c0d776cf2cb91efcc130c23ffb398ebc78b8b0edc52710014d0e3440ba3bf2974fcf99e65fa1dbf1d5f51755b660a90a4cf9a3aba7463cea03207e990eb7b81c15406f8faff33a7232eedb7460f5168856a1270eaa52b7297df2017101000000ab6324696458207e990eb7b81c15406f8faff33a7232eedb7460f5168856a1270eaa52b7297df265247479706566646f6d61696e656c6162656c6461736432677265636f726473a17464617368556e697175654964656e74697479496458205e56add13ec7288fe5204f8690f811d6e629b6e0b51a9dd95633b007f92c5cd968246f776e6572496458205e56add13ec7288fe5204f8690f811d6e629b6e0b51a9dd95633b007f92c5cd969247265766973696f6e016c7072656f7264657253616c745820acd91c6c4f6094fe057ff2a8ac1993e21f66cd00e31b5a8a18cadd59315b7d9e6e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c6461736432781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d65646461736810018f1c3010806bb4057364d7d3761084daf5096886702210098515c3f62324da8611032091724a9c88f5f763d573395ac7f00da96790c18f9189d4b30b6212b5f5be38d0017101000000ab63246964582091724a9c88f5f763d573395ac7f00da96790c18f9189d4b30b6212b5f5be38d065247479706566646f6d61696e656c6162656c6461736431677265636f726473a17464617368556e697175654964656e7469747949645820f1d4984eb577d145e314116a11a49a700426db0322e0943e7a050c3144b33cda68246f776e657249645820f1d4984eb577d145e314116a11a49a700426db0322e0943e7a050c3144b33cda69247265766973696f6e016c7072656f7264657253616c745820ff0fdc12619ebc2265bb3aae0dbc44818cbf10aebaed9ba3005dc74a0716177d6e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c6461736431781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d65646461736810026d6ccb16e7fcf8e34fcd05b5fc035b346ea6f499690e8808280b20509cb8c8e3032092776d5c920b5c8d4de030536af3e0aa2401a9d2a0f3211ac0f66c028a1750c3017101000000ab63246964582092776d5c920b5c8d4de030536af3e0aa2401a9d2a0f3211ac0f66c028a1750c365247479706566646f6d61696e656c6162656c6461736433677265636f726473a17464617368556e697175654964656e746974794964582023054a23cb684475d56fe89db2a8adbc2fe398d4b830f069e5b174a0fd53ab5668246f776e65724964582023054a23cb684475d56fe89db2a8adbc2fe398d4b830f069e5b174a0fd53ab5669247265766973696f6e016c7072656f7264657253616c7458209ca1a950bcbbde43e29eee2cdc874a0cb6456534c4a3b5946e19035d33d5e8f16e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c6461736433781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d656464617368111111111102e325263a20896c0a113daaffb8137cd07064ae43c16bd338f6cc6db0e806e9ae1001d1e338a6d4d6598a1e25a5ab7b9376ed924354155f393538912af979f1583634028674ededae81040d147bd7f4c0dcedfb598919f91a917dd6a6948633ad49cc8b10019d6ba2132bfe234f09256baa9d0029e1a0c70fd5147b4f0d623ddfe8d942be080320ba7bd0c8bfe79f69424170ca99eb0beb76b44db6ed4193544d693f16ad5888b2017301000000ab632469645820ba7bd0c8bfe79f69424170ca99eb0beb76b44db6ed4193544d693f16ad5888b265247479706566646f6d61696e656c6162656c656861736832677265636f726473a17464617368556e697175654964656e7469747949645820a853f0c0555cfda938be70271ce706eb5cfdcfc847184a953ca3449438ece68f68246f776e657249645820a853f0c0555cfda938be70271ce706eb5cfdcfc847184a953ca3449438ece68f69247265766973696f6e016c7072656f7264657253616c745820ed7b8abec90dfe8d0ec952cdf017dec6d89187a923f0b0347cdcb265ccdf12f46e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c656861736832781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d656464617368100185decbb070552a2b74e807e31aae6bd75ad8002c2e6ea164ff57d36b4ff5773c11110320c1ed31d0d049892a5fa2632182882ca21e0a6beb91387d2321dc9e0687164151017301000000ab632469645820c1ed31d0d049892a5fa2632182882ca21e0a6beb91387d2321dc9e068716415165247479706566646f6d61696e656c6162656c656861736831677265636f726473a17464617368556e697175654964656e74697479496458209c561187f7dc36f6061008e19b54fb1e8a64167f3d95b68bfa29675c9fe17cc668246f776e6572496458209c561187f7dc36f6061008e19b54fb1e8a64167f3d95b68bfa29675c9fe17cc669247265766973696f6e016c7072656f7264657253616c7458207e3f9e22ba98eb6ef5a97c866482c132e6495140eebb670189d35599679f26706e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c656861736831781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d656464617368100189e614d24728190a216e891f7f3677280cfdc8a7e4a7af117140378d06267e3702344c5e0791aaf0090faed05a483c35f5eaad699b70e566cbed6130383887cdc51001ad10088365c5e719f5dc8f3bb8576d0fa8b63734c67a46c9f4aac6df3725002c0320ce30d9d5af687c2b06c1ed92a3028a56eb49a0d37383379aab2d45d9b7b0a605017901000000ab632469645820ce30d9d5af687c2b06c1ed92a3028a56eb49a0d37383379aab2d45d9b7b0a60565247479706566646f6d61696e656c6162656c68726f616455736572677265636f726473a17464617368556e697175654964656e7469747949645820b300a24b600bd04fc154e4b93a367c5fb67fcc2486b2b35bf7c33ded27fec4c968246f776e657249645820b300a24b600bd04fc154e4b93a367c5fb67fcc2486b2b35bf7c33ded27fec4c969247265766973696f6e016c7072656f7264657253616c7458202c0a8b47a8509441035b47c2fdf55ac01e06d7fb6b70803e1b01ea1825d9b69a6e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c68726f616475736572781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d6564646173681001293530403a5c19a1eedec83401b3ab789ebaac6265a2989e367cb4fe9d4c16a71111022041f744f1a1a2250c62871172a56da204df9d4d3a9dbe1d87a70c87476ce8081001630d5e88f8a9de4a8b346cd8146e087d95c9cc8aeb48e98f3dbe4f7de58a9752022f6f3166c0b4a24016800b16b7a00324c4c53cc7c76f08bd3055427f1b18f82a100184730abb4e78e51c4cb1145915c00386d24c00353dcce58a608c4339994e190602f1fe93daeee1ddf3b761e993525d128cf12fe2b9043f6f68525ef67e02006bcd100320fa1545588ad51bc2b94f5aa0507a746cbb8ca65c792fd9bfc365ac4344926cc8019101000000ab632469645820fa1545588ad51bc2b94f5aa0507a746cbb8ca65c792fd9bfc365ac4344926cc865247479706566646f6d61696e656c6162656c743061353638356533356661343464363065313863677265636f726473a17464617368556e697175654964656e7469747949645820500325331ad21264af5dee15008d4c0bb9f25583438da77c05fec5a5a57f310f68246f776e657249645820500325331ad21264af5dee15008d4c0bb9f25583438da77c05fec5a5a57f310f69247265766973696f6e016c7072656f7264657253616c745820c34bd433b80a9a234a89c9d04ef4d96500af687dbaf3fc54c12830e88f890c376e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e7472616374496458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486f6e6f726d616c697a65644c6162656c743061353638356533356661343464363065313863781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d6564646173681111111111")
        val signatureLlmqHashData = Converters.fromHex("00000120b5cd6cdd8b85918f291d816e1a14604088f915a61ec4c85832d082ff")
        val signatureData = Converters.fromHex("802fb837b5913e8cfde566b06b2662f07d4eaad4b9d458d9527e31b4f6f0732cb2596e474afc6740cb2b1c80b63900d107f1cbda5a05d0d397a1cdcc8d8d923f11939da4c5f8f84af9cb9e1cbcc412f64e4e71d76dfae9964015bbcb98d1e62b")
        val metaData = Converters.fromHex("08db2210c9a803")

        val proof = Proof(
            PlatformOuterClass.Proof.newBuilder()
                .setMerkleProof(ByteString.copyFrom(rootTreeProofData))
                .setSignature(ByteString.copyFrom(signatureData))
                .setSignatureLlmqHash(ByteString.copyFrom(signatureLlmqHashData))
                // .setStoreTreeProofs(
                //    PlatformOuterClass.StoreTreeProofs.newBuilder().setDocumentsProof(ByteString.copyFrom(documentsProofData))
                // )
                .build()
        )

        assertArrayEquals(proof.signature, signatureData, "Signature must match")
        assertArrayEquals(proof.signatureLlmqHash, signatureLlmqHashData, "Signature quorum must match")
        assertArrayEquals(proof.rootTreeProof, rootTreeProofData, "Root tree proof must match")
        assertArrayEquals(proof.storeTreeProofs.documentsProof, documentsProofData, "Identity tree proof must match")

        // Quorum()
        val quorumEntry = Quorum(
            PARAMS,
            LLMQParameters.fromType(LLMQParameters.LLMQType.LLMQ_DEVNET),
            Sha256Hash.wrapReversed(Converters.fromHex("7f315ea78de78c3ac9b2c089f40138114088963314a3c0101fb7eaaad5000000")),
            BLSPublicKey(PublicKey.FromBytes(Converters.fromHex("0a396fd00ac8f678a242c4b14004fe3402bdb9ada641e48e11ca6be3c87c5858b4cbc6014622d98df95b1a68b1bbd46c")))
        )

        val expectedStateHash = "c0607cbb713a37b4bb352493ea29e30ff301aaaa1951b31644467d695ff4994f".toByteArray()
        var documentsPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null

        val rootElementsToProve = hashMapOf<Int, ByteArray>()
        if (proof.storeTreeProofs.documentsProof.isNotEmpty()) {
            documentsPair = MerkVerifyProof.extractProofWithHash(proof.storeTreeProofs.documentsProof)
            rootElementsToProve[Indices.Documents.value] = documentsPair.first
        }
        val merkleProof = MerkleProof.fromBuffer(proof.rootTreeProof) {
            blake3(it)
        }
        val stateHash = merkleProof.calculateRoot(rootElementsToProve.keys.toList(), rootElementsToProve.values.toList(), 6)
        assertEquals(expectedStateHash.toHex(), stateHash.toHex())

        val responseMetaData = ResponseMetadata(metaData)
        val results = ProofVerifier.verifyAndExtractFromProof(proof, responseMetaData, quorumEntry, "getDocuments")

        assertEquals(results.keys.size, 1)
    }

    // @Test
    fun testDocumentSTProof() {
        val rootTreeProofData = Converters.fromHex("a547c0cde0281ca3c83b26836edcac9eacba608e296a4a9131cde048e929ae04bc21ba06b4bd3bf3bcb2670deaa9215a09aec5ebabd577908e417ede449a7aaf")
        val documentsProofData = Converters.fromHex("01c6b3c30bd15f1f76d2c2653f5ca3c6e895644a59e9d5eae90db6a591ae97b3ec02e325263a20896c0a113daaffb8137cd07064ae43c16bd338f6cc6db0e806e9ae10014a053a88b81c4c0939d6161884c98adc894c5da28f2cb226a68a0a6f98b971ee0213f44c8f87486360ff9c48f75ace608fed5b3190e44413c7581c8ef9c26a5c9f1001ab0ffb0e7a8180719782027e249ba77552cd27c9e445545ec989e4ebe3dc9cf6022041f744f1a1a2250c62871172a56da204df9d4d3a9dbe1d87a70c87476ce8081001efb316ea90a91f89369667e3700a068d04b57e60e8d73e26f57bea432ec4aedc02ccc0bfe89d7efd40d737eebe589ac5b5b5fafc24a9bea6aa087436cbda75f56a100320e3e7e61a8ee3ae7fcbf108341ee12bde3b3cc6de66a3121a4e27570247de715600e001000000a8632469645820e3e7e61a8ee3ae7fcbf108341ee12bde3b3cc6de66a3121a4e27570247de71566524747970656770726f66696c6568246f776e6572496458209c561187f7dc36f6061008e19b54fb1e8a64167f3d95b68bfa29675c9fe17cc669247265766973696f6e18286a246372656174656441741b0000017c18ab36526a247570646174656441741b0000017c4e6e569c6b646973706c61794e616d6569486173682050726f666f2464617461436f6e7472616374496458206d5762f796189a35e45520f3f0b2a4550f7f0310f2406d4dbf632592d6da78251102c5cc273465ca7639e3135c7dd9f4ce7be6638603be19aa231f57d62dd458a69c100193fa6214298429f854625f4d99b909e7c4dceea6c771ee17ac25d401f8a762e011022f6f3166c0b4a24016800b16b7a00324c4c53cc7c76f08bd3055427f1b18f82a1001e440b1938d27736e2a72298d0bba24721472261795924ce52cb09e60370a6ff911111111")
        val signatureLlmqHashData = Converters.fromHex("0000005369ba47b478ff971bba47f8c969291de751aa40cca165e6f5f98aaacc")
        val signatureData = Converters.fromHex("90c841b75cb8f0f6e2fde8b6991f7343cdbe57aff6ab0210ea6070fe52cda4bf50f4849da7e03f7a297a8c6b2f987b8b0ee60804f522b4df168a3c037bf34371a376e9e54d58c186df7f072df40ae142f865e30610553ce1f0102b4d7001f60d")

        val proof = Proof(
            PlatformOuterClass.Proof.newBuilder()
                .setMerkleProof(ByteString.copyFrom(rootTreeProofData))
                .setSignature(ByteString.copyFrom(signatureData))
                .setSignatureLlmqHash(ByteString.copyFrom(signatureLlmqHashData))
                // .setStoreTreeProofs(
                //     PlatformOuterClass.StoreTreeProofs.newBuilder().setDocumentsProof(ByteString.copyFrom(documentsProofData))
                // )
                .build()
        )

        assertArrayEquals(proof.signature, signatureData, "Signature must match")
        assertArrayEquals(proof.signatureLlmqHash, signatureLlmqHashData, "Signature quorum must match")
        assertArrayEquals(proof.rootTreeProof, rootTreeProofData, "Root tree proof must match")
        assertArrayEquals(proof.storeTreeProofs.documentsProof, documentsProofData, "Identity tree proof must match")

        // Quorum()
        val quorumEntry = Quorum(
            PARAMS,
            LLMQParameters.llmq_devnet,
            Sha256Hash.wrap(Converters.fromHex("0000005369ba47b478ff971bba47f8c969291de751aa40cca165e6f5f98aaacc")),
            BLSPublicKey(PublicKey.FromBytes(Converters.fromHex("139ead0c787b4feea1b99217544dd6548c38285d57a6e5e8c7f650e93567ffc783a39bc469e0e4da0cc36a427bab9ae4")))
        )

        val expectedStateHash = "20913237681b434bd2cc6a423975a064e34b74b024edd02fcf034fa77a0c387f".toByteArray()
        var documentsPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null

        val rootElementsToProve = hashMapOf<Int, ByteArray>()
        if (proof.storeTreeProofs.documentsProof.isNotEmpty()) {
            documentsPair = MerkVerifyProof.extractProofWithHash(proof.storeTreeProofs.documentsProof)
            rootElementsToProve[Indices.Documents.value] = documentsPair.first
        }
        val merkleProof = MerkleProof.fromBuffer(proof.rootTreeProof) {
            blake3(it)
        }
        val stateHash = merkleProof.calculateRoot(rootElementsToProve.keys.toList(), rootElementsToProve.values.toList(), 6)
        assertEquals(expectedStateHash.toHex(), stateHash.toHex())

        val responseMetaData = ResponseMetadata(6212, 56574)
        val results = ProofVerifier.verifyAndExtractFromProof(proof, responseMetaData, quorumEntry, "broadcast")

        assertEquals(results.keys.size, 1)
    }

    // @Test
    fun testDataContractProof() {
        // INFO: grpcRequest(GetContractMethod, -1, null, false) with 34.213.172.28 for getContract(8F4WqzVuqyYEBMR1AraBuYG1cjk3hqUYdzLSMdYpWLbH, true)
        // Oct 01, 2021 9:02:37 PM org.dashj.platform.dapiclient.DapiClient getDataContract
        // INFO: proof = Proof(rootTreeProof: 5c9e573398405996c0d6f4e9a7a49e7c5df8963d98c843ca5b3cf84b65b333ddc038c0ee74443899662353eac640353ce6509288c2c5fcbf6e8386f9d38142ce072907f22609678cb56cacace7bbd6ca9d7b6db1effd1d674a83cab37d9d40ba
        // storeTreeProof: StoreTreeProofs ->
        // dataContractsProof: 017729c902352f320b28fe8e5f65a29013949e4279681b5e38fc1b0fe543fde7a203206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a480c9401000000a46324696458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486724736368656d61783468747470733a2f2f736368656d612e646173682e6f72672f6470702d302d342d302f6d6574612f646174612d636f6e7472616374676f776e6572496458203e9f0e8a33ac81fbfa1cd4dc2685800c5dc1288d58eb53b6179e7579005299da69646f63756d656e7473a266646f6d61696ea66474797065666f626a65637467696e646963657383a266756e69717565f56a70726f7065727469657382a1781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d6563617363a16f6e6f726d616c697a65644c6162656c63617363a266756e69717565f56a70726f7065727469657381a1781c7265636f7264732e64617368556e697175654964656e74697479496463617363a16a70726f7065727469657381a1781b7265636f7264732e64617368416c6961734964656e746974794964636173636824636f6d6d656e74790137496e206f7264657220746f207265676973746572206120646f6d61696e20796f75206e65656420746f206372656174652061207072656f726465722e20546865207072656f726465722073746570206973206e656564656420746f2070726576656e74206d616e2d696e2d7468652d6d6964646c652061747461636b732e206e6f726d616c697a65644c6162656c202b20272e27202b206e6f726d616c697a6564506172656e74446f6d61696e206d757374206e6f74206265206c6f6e676572207468616e20323533206368617273206c656e67746820617320646566696e65642062792052464320313033352e20446f6d61696e20646f63756d656e74732061726520696d6d757461626c653a206d6f64696669636174696f6e20616e642064656c6574696f6e20617265207265737472696374656468726571756972656486656c6162656c6f6e6f726d616c697a65644c6162656c781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d656c7072656f7264657253616c74677265636f7264736e737562646f6d61696e52756c65736a70726f70657274696573a6656c6162656ca5647479706566737472696e67677061747465726e782a5e5b612d7a412d5a302d395d5b612d7a412d5a302d392d5d7b302c36317d5b612d7a412d5a302d395d24696d61784c656e677468183f696d696e4c656e677468036b6465736372697074696f6e7819446f6d61696e206c6162656c2e20652e672e2027426f62272e677265636f726473a66474797065666f626a6563746824636f6d6d656e747890436f6e73747261696e742077697468206d617820616e64206d696e2070726f7065727469657320656e737572652074686174206f6e6c79206f6e65206964656e74697479207265636f72642069732075736564202d206569746865722061206064617368556e697175654964656e74697479496460206f722061206064617368416c6961734964656e746974794964606a70726f70657274696573a27364617368416c6961734964656e746974794964a764747970656561727261796824636f6d6d656e7478234d75737420626520657175616c20746f2074686520646f63756d656e74206f776e6572686d61784974656d731820686d696e4974656d73182069627974654172726179f56b6465736372697074696f6e783d4964656e7469747920494420746f206265207573656420746f2063726561746520616c696173206e616d657320666f7220746865204964656e7469747970636f6e74656e744d656469615479706578216170706c69636174696f6e2f782e646173682e6470702e6964656e7469666965727464617368556e697175654964656e746974794964a764747970656561727261796824636f6d6d656e7478234d75737420626520657175616c20746f2074686520646f63756d656e74206f776e6572686d61784974656d731820686d696e4974656d73182069627974654172726179f56b6465736372697074696f6e783e4964656e7469747920494420746f206265207573656420746f2063726561746520746865207072696d617279206e616d6520746865204964656e7469747970636f6e74656e744d656469615479706578216170706c69636174696f6e2f782e646173682e6470702e6964656e7469666965726d6d617850726f70657274696573016d6d696e50726f7065727469657301746164646974696f6e616c50726f70657274696573f46c7072656f7264657253616c74a56474797065656172726179686d61784974656d731820686d696e4974656d73182069627974654172726179f56b6465736372697074696f6e782253616c74207573656420696e20746865207072656f7264657220646f63756d656e746e737562646f6d61696e52756c6573a56474797065666f626a656374687265717569726564816f616c6c6f77537562646f6d61696e736a70726f70657274696573a16f616c6c6f77537562646f6d61696e73a3647479706567626f6f6c65616e6824636f6d6d656e74784f4f6e6c792074686520646f6d61696e206f776e657220697320616c6c6f77656420746f2063726561746520737562646f6d61696e7320666f72206e6f6e20746f702d6c6576656c20646f6d61696e736b6465736372697074696f6e785b54686973206f7074696f6e20646566696e65732077686f2063616e2063726561746520737562646f6d61696e733a2074727565202d20616e796f6e653b2066616c7365202d206f6e6c792074686520646f6d61696e206f776e65726b6465736372697074696f6e7842537562646f6d61696e2072756c657320616c6c6f7720646f6d61696e206f776e65727320746f20646566696e652072756c657320666f7220737562646f6d61696e73746164646974696f6e616c50726f70657274696573f46f6e6f726d616c697a65644c6162656ca5647479706566737472696e67677061747465726e78215e5b612d7a302d395d5b612d7a302d392d5d7b302c36317d5b612d7a302d395d246824636f6d6d656e7478694d75737420626520657175616c20746f20746865206c6162656c20696e206c6f776572636173652e20546869732070726f70657274792077696c6c20626520646570726563617465642064756520746f206361736520696e73656e73697469766520696e6469636573696d61784c656e677468183f6b6465736372697074696f6e7850446f6d61696e206c6162656c20696e206c6f7765726361736520666f7220636173652d696e73656e73697469766520756e697175656e6573732076616c69646174696f6e2e20652e672e2027626f6227781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d65a6647479706566737472696e67677061747465726e78285e247c5e5b5b612d7a302d395d5b612d7a302d392d5c2e5d7b302c3138387d5b612d7a302d395d246824636f6d6d656e74788c4d7573742065697468657220626520657175616c20746f20616e206578697374696e6720646f6d61696e206f7220656d70747920746f20637265617465206120746f70206c6576656c20646f6d61696e2e204f6e6c7920746865206461746120636f6e7472616374206f776e65722063616e2063726561746520746f70206c6576656c20646f6d61696e732e696d61784c656e67746818be696d696e4c656e677468006b6465736372697074696f6e785e412066756c6c20706172656e7420646f6d61696e206e616d6520696e206c6f7765726361736520666f7220636173652d696e73656e73697469766520756e697175656e6573732076616c69646174696f6e2e20652e672e20276461736827746164646974696f6e616c50726f70657274696573f4687072656f72646572a66474797065666f626a65637467696e646963657381a266756e69717565f56a70726f7065727469657381a17073616c746564446f6d61696e48617368636173636824636f6d6d656e74784a5072656f7264657220646f63756d656e74732061726520696d6d757461626c653a206d6f64696669636174696f6e20616e642064656c6574696f6e206172652072657374726963746564687265717569726564817073616c746564446f6d61696e486173686a70726f70657274696573a17073616c746564446f6d61696e48617368a56474797065656172726179686d61784974656d731820686d696e4974656d73182069627974654172726179f56b6465736372697074696f6e7859446f75626c65207368612d323536206f662074686520636f6e636174656e6174696f6e206f66206120333220627974652072616e646f6d2073616c7420616e642061206e6f726d616c697a656420646f6d61696e206e616d65746164646974696f6e616c50726f70657274696573f410012b11d865d0cd0cdb032c3372b647b7160768d45c177dd823625e38f12e81f5ca11
        // signatureLlmqHash: 00000120b5cd6cdd8b85918f291d816e1a14604088f915a61ec4c85832d082ff
        // signature: 176b0050173bd347b8149e28850cd6ac919dc04972d708e30c180f013989d0823201e42262334cfb8d52ab2a5464e9561031eb328bfd631495f60d9b466761c956e7301c37d3b84cab51e09fa76a3c6e9ea6cdbb47655e48503ba2a3bd2c613b

        val rootTreeProofData = Converters.fromHex("5c9e573398405996c0d6f4e9a7a49e7c5df8963d98c843ca5b3cf84b65b333ddc038c0ee74443899662353eac640353ce6509288c2c5fcbf6e8386f9d38142ce072907f22609678cb56cacace7bbd6ca9d7b6db1effd1d674a83cab37d9d40ba")
        val contractsProofData = Converters.fromHex("017729c902352f320b28fe8e5f65a29013949e4279681b5e38fc1b0fe543fde7a203206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a480c9401000000a46324696458206b9be9eaeeee1b970a367dc45caf718914ca0500e08726cb0288cd70219c0a486724736368656d61783468747470733a2f2f736368656d612e646173682e6f72672f6470702d302d342d302f6d6574612f646174612d636f6e7472616374676f776e6572496458203e9f0e8a33ac81fbfa1cd4dc2685800c5dc1288d58eb53b6179e7579005299da69646f63756d656e7473a266646f6d61696ea66474797065666f626a65637467696e646963657383a266756e69717565f56a70726f7065727469657382a1781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d6563617363a16f6e6f726d616c697a65644c6162656c63617363a266756e69717565f56a70726f7065727469657381a1781c7265636f7264732e64617368556e697175654964656e74697479496463617363a16a70726f7065727469657381a1781b7265636f7264732e64617368416c6961734964656e746974794964636173636824636f6d6d656e74790137496e206f7264657220746f207265676973746572206120646f6d61696e20796f75206e65656420746f206372656174652061207072656f726465722e20546865207072656f726465722073746570206973206e656564656420746f2070726576656e74206d616e2d696e2d7468652d6d6964646c652061747461636b732e206e6f726d616c697a65644c6162656c202b20272e27202b206e6f726d616c697a6564506172656e74446f6d61696e206d757374206e6f74206265206c6f6e676572207468616e20323533206368617273206c656e67746820617320646566696e65642062792052464320313033352e20446f6d61696e20646f63756d656e74732061726520696d6d757461626c653a206d6f64696669636174696f6e20616e642064656c6574696f6e20617265207265737472696374656468726571756972656486656c6162656c6f6e6f726d616c697a65644c6162656c781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d656c7072656f7264657253616c74677265636f7264736e737562646f6d61696e52756c65736a70726f70657274696573a6656c6162656ca5647479706566737472696e67677061747465726e782a5e5b612d7a412d5a302d395d5b612d7a412d5a302d392d5d7b302c36317d5b612d7a412d5a302d395d24696d61784c656e677468183f696d696e4c656e677468036b6465736372697074696f6e7819446f6d61696e206c6162656c2e20652e672e2027426f62272e677265636f726473a66474797065666f626a6563746824636f6d6d656e747890436f6e73747261696e742077697468206d617820616e64206d696e2070726f7065727469657320656e737572652074686174206f6e6c79206f6e65206964656e74697479207265636f72642069732075736564202d206569746865722061206064617368556e697175654964656e74697479496460206f722061206064617368416c6961734964656e746974794964606a70726f70657274696573a27364617368416c6961734964656e746974794964a764747970656561727261796824636f6d6d656e7478234d75737420626520657175616c20746f2074686520646f63756d656e74206f776e6572686d61784974656d731820686d696e4974656d73182069627974654172726179f56b6465736372697074696f6e783d4964656e7469747920494420746f206265207573656420746f2063726561746520616c696173206e616d657320666f7220746865204964656e7469747970636f6e74656e744d656469615479706578216170706c69636174696f6e2f782e646173682e6470702e6964656e7469666965727464617368556e697175654964656e746974794964a764747970656561727261796824636f6d6d656e7478234d75737420626520657175616c20746f2074686520646f63756d656e74206f776e6572686d61784974656d731820686d696e4974656d73182069627974654172726179f56b6465736372697074696f6e783e4964656e7469747920494420746f206265207573656420746f2063726561746520746865207072696d617279206e616d6520746865204964656e7469747970636f6e74656e744d656469615479706578216170706c69636174696f6e2f782e646173682e6470702e6964656e7469666965726d6d617850726f70657274696573016d6d696e50726f7065727469657301746164646974696f6e616c50726f70657274696573f46c7072656f7264657253616c74a56474797065656172726179686d61784974656d731820686d696e4974656d73182069627974654172726179f56b6465736372697074696f6e782253616c74207573656420696e20746865207072656f7264657220646f63756d656e746e737562646f6d61696e52756c6573a56474797065666f626a656374687265717569726564816f616c6c6f77537562646f6d61696e736a70726f70657274696573a16f616c6c6f77537562646f6d61696e73a3647479706567626f6f6c65616e6824636f6d6d656e74784f4f6e6c792074686520646f6d61696e206f776e657220697320616c6c6f77656420746f2063726561746520737562646f6d61696e7320666f72206e6f6e20746f702d6c6576656c20646f6d61696e736b6465736372697074696f6e785b54686973206f7074696f6e20646566696e65732077686f2063616e2063726561746520737562646f6d61696e733a2074727565202d20616e796f6e653b2066616c7365202d206f6e6c792074686520646f6d61696e206f776e65726b6465736372697074696f6e7842537562646f6d61696e2072756c657320616c6c6f7720646f6d61696e206f776e65727320746f20646566696e652072756c657320666f7220737562646f6d61696e73746164646974696f6e616c50726f70657274696573f46f6e6f726d616c697a65644c6162656ca5647479706566737472696e67677061747465726e78215e5b612d7a302d395d5b612d7a302d392d5d7b302c36317d5b612d7a302d395d246824636f6d6d656e7478694d75737420626520657175616c20746f20746865206c6162656c20696e206c6f776572636173652e20546869732070726f70657274792077696c6c20626520646570726563617465642064756520746f206361736520696e73656e73697469766520696e6469636573696d61784c656e677468183f6b6465736372697074696f6e7850446f6d61696e206c6162656c20696e206c6f7765726361736520666f7220636173652d696e73656e73697469766520756e697175656e6573732076616c69646174696f6e2e20652e672e2027626f6227781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d65a6647479706566737472696e67677061747465726e78285e247c5e5b5b612d7a302d395d5b612d7a302d392d5c2e5d7b302c3138387d5b612d7a302d395d246824636f6d6d656e74788c4d7573742065697468657220626520657175616c20746f20616e206578697374696e6720646f6d61696e206f7220656d70747920746f20637265617465206120746f70206c6576656c20646f6d61696e2e204f6e6c7920746865206461746120636f6e7472616374206f776e65722063616e2063726561746520746f70206c6576656c20646f6d61696e732e696d61784c656e67746818be696d696e4c656e677468006b6465736372697074696f6e785e412066756c6c20706172656e7420646f6d61696e206e616d6520696e206c6f7765726361736520666f7220636173652d696e73656e73697469766520756e697175656e6573732076616c69646174696f6e2e20652e672e20276461736827746164646974696f6e616c50726f70657274696573f4687072656f72646572a66474797065666f626a65637467696e646963657381a266756e69717565f56a70726f7065727469657381a17073616c746564446f6d61696e48617368636173636824636f6d6d656e74784a5072656f7264657220646f63756d656e74732061726520696d6d757461626c653a206d6f64696669636174696f6e20616e642064656c6574696f6e206172652072657374726963746564687265717569726564817073616c746564446f6d61696e486173686a70726f70657274696573a17073616c746564446f6d61696e48617368a56474797065656172726179686d61784974656d731820686d696e4974656d73182069627974654172726179f56b6465736372697074696f6e7859446f75626c65207368612d323536206f662074686520636f6e636174656e6174696f6e206f66206120333220627974652072616e646f6d2073616c7420616e642061206e6f726d616c697a656420646f6d61696e206e616d65746164646974696f6e616c50726f70657274696573f410012b11d865d0cd0cdb032c3372b647b7160768d45c177dd823625e38f12e81f5ca11")
        val signatureLlmqHashData = Converters.fromHex("00000120b5cd6cdd8b85918f291d816e1a14604088f915a61ec4c85832d082ff")
        val signatureData = Converters.fromHex("176b0050173bd347b8149e28850cd6ac919dc04972d708e30c180f013989d0823201e42262334cfb8d52ab2a5464e9561031eb328bfd631495f60d9b466761c956e7301c37d3b84cab51e09fa76a3c6e9ea6cdbb47655e48503ba2a3bd2c613b")
        val metaData = Converters.fromHex("08db2210c9a803")

        val proof = Proof(
            PlatformOuterClass.Proof.newBuilder()
                .setMerkleProof(ByteString.copyFrom(rootTreeProofData))
                .setSignature(ByteString.copyFrom(signatureData))
                .setSignatureLlmqHash(ByteString.copyFrom(signatureLlmqHashData))
                // .setStoreTreeProofs(
                //     PlatformOuterClass.StoreTreeProofs.newBuilder().setDataContractsProof(ByteString.copyFrom(contractsProofData))
                // )
                .build()
        )

        assertArrayEquals(proof.signature, signatureData, "Signature must match")
        assertArrayEquals(proof.signatureLlmqHash, signatureLlmqHashData, "Signature quorum must match")
        assertArrayEquals(proof.rootTreeProof, rootTreeProofData, "Root tree proof must match")
        assertArrayEquals(proof.storeTreeProofs.dataContractsProof, contractsProofData, "Data Contract tree proof must match")

        // Quorum()
        val quorumEntry = Quorum(
            PARAMS,
            LLMQParameters.llmq_devnet,
            Sha256Hash.wrapReversed(Converters.fromHex("7f315ea78de78c3ac9b2c089f40138114088963314a3c0101fb7eaaad5000000")),
            BLSPublicKey(PublicKey.FromBytes(Converters.fromHex("0a396fd00ac8f678a242c4b14004fe3402bdb9ada641e48e11ca6be3c87c5858b4cbc6014622d98df95b1a68b1bbd46c")))
        )

        val expectedStateHash = "c0607cbb713a37b4bb352493ea29e30ff301aaaa1951b31644467d695ff4994f".toByteArray()
        var contractsPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null

        val rootElementsToProve = hashMapOf<Int, ByteArray>()
        if (proof.storeTreeProofs.dataContractsProof.isNotEmpty()) {
            contractsPair = MerkVerifyProof.extractProofWithHash(proof.storeTreeProofs.dataContractsProof)
            rootElementsToProve[Indices.Contracts.value] = contractsPair.first
        }
        val merkleProof = MerkleProof.fromBuffer(proof.rootTreeProof) {
            blake3(it)
        }
        val stateHash = merkleProof.calculateRoot(rootElementsToProve.keys.toList(), rootElementsToProve.values.toList(), 6)
        assertEquals(expectedStateHash.toHex(), stateHash.toHex())

        val responseMetaData = ResponseMetadata(metaData)
        val results = ProofVerifier.verifyAndExtractFromProof(proof, responseMetaData, quorumEntry, "getDataContract")

        assertEquals(results.keys.size, 1)
    }

    // @Test
    fun testIdentityProof() {
        /*
        INFO: proof = Proof(rootTreeProof: bfef7d172b666943c33fae47b614259412f52435edd99bbf933144411c3aeab49b901c60efbd5040ab1122197418963b88d06dc440b88e02efca9292f0f0f275072907f22609678cb56cacace7bbd6ca9d7b6db1effd1d674a83cab37d9d40ba
        storeTreeProof: StoreTreeProofs ->
        identitiesProof: 01be2d38ffd3439ff8d039c1ae935700844bd14c7a99f7d0d2d4945bc782a7fec3028658e68fc28d13b09dd6cac7897df33f607e56f1c6bd207082c32af9dc89305810012f2c986fdfb5a1954dd5fc0fecf125f9b3e27d320e1f53957270eefbd727360a02bcadfc833a39b7497aaa993af34d9b0fb45b6b4ab77df96aa378601707fa9735100174a5d6f09b230c09dbf1c015cfd3880e8c088edf97b38aad7670a77060dc894703203e9f0e8a33ac81fbfa1cd4dc2685800c5dc1288d58eb53b6179e7579005299da007e01000000a462696458203e9f0e8a33ac81fbfa1cd4dc2685800c5dc1288d58eb53b6179e7579005299da6762616c616e6365192c5c687265766973696f6e006a7075626c69634b65797381a362696400646461746158210286be3e3248056ddae8f73bfd04e149a9b98f6632277d8fd56077fffaafd7cf726474797065001001510d76cde92eeda7e86ef1b4dc8caea7ca002fb4cc46d16006830a0424aa1a3311111102887fcd3a7fef9b356dd12fc2e4d58c54c9e42908070dee2aaf7c7b5d389f736010018004cb6cefede5289b75f661279735cfe374d1b7f12e9840e1c9d1dc0e7c24461102c97ff70a287f4d9741f5c54e5fc5e6a365043cdbedf623ae7d0e280a6a32b70b10018a28f5bebdbf987079878315cde74e22ef591983a576d3c6e2807ae1fd12ff8811

        signatureLlmqHash: 00000015d26abafc6844e8d1ff95244cda2c7f1b12c1beb629eaf22348f60310
        signature: 0a03ec4d401a4fad7235e1f5b0770114a221bd8811e24bc7eb5f9ea68a52af1ba12612d07827fdcd367df3e93da914be09a948e380b63d11a9422491f8ff4ffda38d70456ac72273d6f101dd3a265690e3f5036bc18c40d4a612916644250add
         */
        val rootTreeProofData = Converters.fromHex("bfef7d172b666943c33fae47b614259412f52435edd99bbf933144411c3aeab49b901c60efbd5040ab1122197418963b88d06dc440b88e02efca9292f0f0f275072907f22609678cb56cacace7bbd6ca9d7b6db1effd1d674a83cab37d9d40ba")
        val identityProofData = Converters.fromHex("01be2d38ffd3439ff8d039c1ae935700844bd14c7a99f7d0d2d4945bc782a7fec3028658e68fc28d13b09dd6cac7897df33f607e56f1c6bd207082c32af9dc89305810012f2c986fdfb5a1954dd5fc0fecf125f9b3e27d320e1f53957270eefbd727360a02bcadfc833a39b7497aaa993af34d9b0fb45b6b4ab77df96aa378601707fa9735100174a5d6f09b230c09dbf1c015cfd3880e8c088edf97b38aad7670a77060dc894703203e9f0e8a33ac81fbfa1cd4dc2685800c5dc1288d58eb53b6179e7579005299da007e01000000a462696458203e9f0e8a33ac81fbfa1cd4dc2685800c5dc1288d58eb53b6179e7579005299da6762616c616e6365192c5c687265766973696f6e006a7075626c69634b65797381a362696400646461746158210286be3e3248056ddae8f73bfd04e149a9b98f6632277d8fd56077fffaafd7cf726474797065001001510d76cde92eeda7e86ef1b4dc8caea7ca002fb4cc46d16006830a0424aa1a3311111102887fcd3a7fef9b356dd12fc2e4d58c54c9e42908070dee2aaf7c7b5d389f736010018004cb6cefede5289b75f661279735cfe374d1b7f12e9840e1c9d1dc0e7c24461102c97ff70a287f4d9741f5c54e5fc5e6a365043cdbedf623ae7d0e280a6a32b70b10018a28f5bebdbf987079878315cde74e22ef591983a576d3c6e2807ae1fd12ff8811")
        val signatureLlmqHashData = Converters.fromHex("00000015d26abafc6844e8d1ff95244cda2c7f1b12c1beb629eaf22348f60310")
        val signatureData = Converters.fromHex("0a03ec4d401a4fad7235e1f5b0770114a221bd8811e24bc7eb5f9ea68a52af1ba12612d07827fdcd367df3e93da914be09a948e380b63d11a9422491f8ff4ffda38d70456ac72273d6f101dd3a265690e3f5036bc18c40d4a612916644250add")
        val metaData = Converters.fromHex("08db2210c9a803")

        val proof = Proof(
            PlatformOuterClass.Proof.newBuilder()
                .setMerkleProof(ByteString.copyFrom(rootTreeProofData))
                .setSignature(ByteString.copyFrom(signatureData))
                .setSignatureLlmqHash(ByteString.copyFrom(signatureLlmqHashData))
                // .setStoreTreeProofs(
                //     PlatformOuterClass.StoreTreeProofs.newBuilder().setIdentitiesProof(ByteString.copyFrom(identityProofData))
                // )
                .build()
        )

        assertArrayEquals(proof.signature, signatureData, "Signature must match")
        assertArrayEquals(proof.signatureLlmqHash, signatureLlmqHashData, "Signature quorum must match")
        assertArrayEquals(proof.rootTreeProof, rootTreeProofData, "Root tree proof must match")
        assertArrayEquals(proof.storeTreeProofs.identitiesProof, identityProofData, "Data Contract tree proof must match")

        // Quorum()
        val quorumEntry = Quorum(
            PARAMS,
            LLMQParameters.llmq_devnet,
            Sha256Hash.wrapReversed(Converters.fromHex("7f315ea78de78c3ac9b2c089f40138114088963314a3c0101fb7eaaad5000000")),
            BLSPublicKey(PublicKey.FromBytes(Converters.fromHex("0a396fd00ac8f678a242c4b14004fe3402bdb9ada641e48e11ca6be3c87c5858b4cbc6014622d98df95b1a68b1bbd46c")))
        )

        val expectedStateHash = "c0607cbb713a37b4bb352493ea29e30ff301aaaa1951b31644467d695ff4994f".toByteArray()
        var contractsPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null

        val rootElementsToProve = hashMapOf<Int, ByteArray>()
        if (proof.storeTreeProofs.identitiesProof.isNotEmpty()) {
            contractsPair = MerkVerifyProof.extractProofWithHash(proof.storeTreeProofs.identitiesProof)
            rootElementsToProve[Indices.Identities.value] = contractsPair.first
        }
        println("inc-rootElementsToProve: ${rootElementsToProve.values.map { it.toHex() }}")
        val merkleProof = MerkleProof.fromBuffer(proof.rootTreeProof) {
            blake3(it)
        }
        val stateHash = merkleProof.calculateRoot(rootElementsToProve.keys.toList(), rootElementsToProve.values.toList(), 6)
        assertEquals(expectedStateHash.toHex(), stateHash.toHex())

        val responseMetaData = ResponseMetadata(metaData)
        val results = ProofVerifier.verifyAndExtractFromProof(proof, responseMetaData, quorumEntry, "getIdentity")

        assertEquals(results.keys.size, 1)
    }

    // @Test
    fun testIdsProof() {
        val rootTreeProofData = Converters.fromHex("bfef7d172b666943c33fae47b614259412f52435edd99bbf933144411c3aeab44c9a148f2e7923672f06313c871cfcd6305dea3ba71ef8f6963d1db1a564980c072907f22609678cb56cacace7bbd6ca9d7b6db1effd1d674a83cab37d9d40ba")
        val idProofData = Converters.fromHex("01f5935375cf59fffdb6e1a952095920fc6c3be6e40ac4d544e54a1c04d72029ac02c97ff70a287f4d9741f5c54e5fc5e6a365043cdbedf623ae7d0e280a6a32b70b1001405f78f596717989e8df803a401f00329dcf81b3f685bd231c394d8d3c669fef0240f954788acef8a2249cc38f8f463c25ae16b02aba043ba4f8371d62919ad21810029ca285f4479486a57ccf99b1fec5ea3d169a1cce0a85a5800336b02c14229c6903209c561187f7dc36f6061008e19b54fb1e8a64167f3d95b68bfa29675c9fe17cc6008001000000a462696458209c561187f7dc36f6061008e19b54fb1e8a64167f3d95b68bfa29675c9fe17cc66762616c616e63651a3b9aa680687265766973696f6e006a7075626c69634b65797381a3626964006464617461582102e7c05f6c66cc559764f600e2cedcde8c6bf49d168783761c5492218aa0b1c7a56474797065001111027f39f41ed72cdee1a84117a520a0e92ad615ad73884c9e752f5d27593d9f7eea1001114f06e497bcc272aa13d7c8e7b185283180a4dba5549331fa599d6f62ac139811021ff2e1078cea7c2c27daf05eed4a93d222105b894df3a39f0f2dbca7102ea9e31001be87efded4c0b8be1e396ae07aed3f332c13929a04745e531912dd21f18d25691111")
        val pkProofData = Converters.fromHex("0144375afff77a7f46f54f79d9ab279c713b46dcdd17faab06c1eb135c58ee30fd0288d896e2cc759ead1368de8ca246123a02199ea11dd03c4a3786f133e5e86b111001df7ed72838bd21fd26aadbbd8505ec5cf08335c041c1868fde0fe5be38e6a05611")

        val signatureLlmqHashData = Converters.fromHex("00000120b5cd6cdd8b85918f291d816e1a14604088f915a61ec4c85832d082ff")
        val signatureData = Converters.fromHex("176b0050173bd347b8149e28850cd6ac919dc04972d708e30c180f013989d0823201e42262334cfb8d52ab2a5464e9561031eb328bfd631495f60d9b466761c956e7301c37d3b84cab51e09fa76a3c6e9ea6cdbb47655e48503ba2a3bd2c613b")
        val metaData = Converters.fromHex("08db2210c9a803")

        val proof = Proof(
            PlatformOuterClass.Proof.newBuilder()
                .setMerkleProof(ByteString.copyFrom(rootTreeProofData))
                .setSignature(ByteString.copyFrom(signatureData))
                .setSignatureLlmqHash(ByteString.copyFrom(signatureLlmqHashData))
                // .setStoreTreeProofs(
                //     PlatformOuterClass.StoreTreeProofs.newBuilder().setIdentitiesProof(ByteString.copyFrom(idProofData))
                //         .setPublicKeyHashesToIdentityIdsProof(ByteString.copyFrom(pkProofData))
                // )
                .build()
        )

        assertArrayEquals(proof.signature, signatureData, "Signature must match")
        assertArrayEquals(proof.signatureLlmqHash, signatureLlmqHashData, "Signature quorum must match")
        assertArrayEquals(proof.rootTreeProof, rootTreeProofData, "Root tree proof must match")
        assertArrayEquals(proof.storeTreeProofs.identitiesProof, idProofData, "Identity tree proof must match")
        assertArrayEquals(proof.storeTreeProofs.publicKeyHashesToIdentityIdsProof, pkProofData, "PublicKey tree proof must match")

        // Quorum()
        val quorumEntry = Quorum(
            PARAMS,
            LLMQParameters.llmq_devnet,
            Sha256Hash.wrapReversed(Converters.fromHex("7f315ea78de78c3ac9b2c089f40138114088963314a3c0101fb7eaaad5000000")),
            BLSPublicKey(PublicKey.FromBytes(Converters.fromHex("0a396fd00ac8f678a242c4b14004fe3402bdb9ada641e48e11ca6be3c87c5858b4cbc6014622d98df95b1a68b1bbd46c")))
        )

        val expectedStateHash = "c0607cbb713a37b4bb352493ea29e30ff301aaaa1951b31644467d695ff4994f".toByteArray()
        var idsPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null
        var pksPair: Pair<ByteArray, Map<ByteArrayKey, ByteArray>>? = null

        val rootElementsToProve = hashMapOf<Int, ByteArray>()
        if (proof.storeTreeProofs.identitiesProof.isNotEmpty()) {
            idsPair = MerkVerifyProof.extractProofWithHash(proof.storeTreeProofs.identitiesProof)
            rootElementsToProve[Indices.Identities.value] = idsPair.first
        }
        if (proof.storeTreeProofs.publicKeyHashesToIdentityIdsProof.isNotEmpty()) {
            pksPair = MerkVerifyProof.extractProofWithHash(proof.storeTreeProofs.publicKeyHashesToIdentityIdsProof)
            rootElementsToProve[Indices.PublicKeyHashesToIdentityIds.value] = pksPair.first
        }

        val merkleProof = MerkleProof.fromBuffer(proof.rootTreeProof) {
            blake3(it)
        }
        val stateHash = merkleProof.calculateRoot(rootElementsToProve.keys.toList(), rootElementsToProve.values.toList(), 6)
        assertEquals(expectedStateHash.toHex(), stateHash.toHex())

        val responseMetaData = ResponseMetadata(metaData)
        val results = ProofVerifier.verifyAndExtractFromProof(proof, responseMetaData, quorumEntry, "getIdentities")

        assertEquals(results.keys.size, 2)
    }

    // @Test // this test is out of date with Platform 0.22
    fun `Root Tree Proof should be correct for all endpoints`() {
        // This test requests all endpoints instead of having multiple test for each endpoint
        // on purpose.
        //
        // The reason being is that when verifying merkle proof, you usually need some value to
        // compare it to, and platform doesn't provide one. There are two ways to verify that
        // the root tree proof is working: either by knowing its root in advance, or by
        // verifying it's signature that is also included in the response.
        // Verifying signature requires verifying the header chain, which is not currently implemented
        // in the JS SDK (Although it is implemented in Java and iOS SDK).
        // So we left with only one option: to know the proof in advance.
        // Platform doesn't give it directly, but we can reconstruct it from
        // store tree leaves. This if fine in this case because this test doesn't test
        // store tree proofs (every endpoint has its own separate store tree proof test).
        // By making requests to all endpoints we can recover all leaves hashes, and construct
        // the original root tree from it. Then we can get the root from that tree and use it
        // as a reference root when verifying the root tree proof.

        val dapiClient = DapiClient(masternodeList.toList(), dpp)
        val stateRepository = StateRepositoryMock()
        val identity = IdentityFactory(
            DashPlatformProtocol(stateRepository), stateRepository
        ).createFromBuffer(dapiClient.getIdentity(identityId.toBuffer()).identity)

        val identityResponse = dapiClient.getIdentity(identityId.toBuffer(), prove = true)
        val keysResponse = dapiClient.getIdentityIdsByPublicKeyHashes(listOf(Utils.sha256hash160(identity.getPublicKeyById(0)!!.data)), true)
        val contractsResponse = dapiClient.getDataContract(dpnsContractId.toBuffer(), true)

        val documentsResponse = dapiClient.getDocuments(
            dpnsContractId.toBuffer(), "preorder",
            DocumentQuery.builder().where("\$id", "==", identityId.toString()).build(), true
        )
        val identitiesByPublicKeyHashesResponse = dapiClient.getIdentitiesByPublicKeyHashes(
            listOf(identity.getPublicKeyById(0)!!.data), true
        )

        val identityProof = MerkleProof.fromBuffer(
            identityResponse.proof!!.rootTreeProof
        ) { blake3(it) }

        val contractsProof = MerkleProof.fromBuffer(
            contractsResponse.proof!!.rootTreeProof
        ) { blake3(it) }

        val documentsProof = MerkleProof.fromBuffer(
            documentsResponse.proof!!.rootTreeProof
        ) { blake3(it) }

        val keysProof = MerkleProof.fromBuffer(
            keysResponse.proof!!.rootTreeProof
        ) { blake3(it) }

        val identitiesByPublicKeyHashesProof = MerkleProof.fromBuffer(
            identitiesByPublicKeyHashesResponse.proof!!.rootTreeProof
        ) { blake3(it) }

        val identityLeaf = MerkVerifyProof.extractProofWithHash(identityResponse.proof!!.storeTreeProofs.identitiesProof).first
        val publicKeysLeaf = MerkVerifyProof.extractProofWithHash(keysResponse.proof!!.storeTreeProofs.publicKeyHashesToIdentityIdsProof).first
        val contractsLeaf = MerkVerifyProof.extractProofWithHash(contractsResponse.proof!!.storeTreeProofs.dataContractsProof).first
        val documentsLeaf = MerkVerifyProof.extractProofWithHash(documentsResponse.proof!!.storeTreeProofs.documentsProof).first

        val reconstructedLeaves = arrayListOf<ByteArray>(
            identityProof.proofHashes[0],
            identityLeaf,
            publicKeysLeaf,
            contractsLeaf,
            documentsLeaf,
            documentsProof.proofHashes[0]
        )

        val reconstructedTree = MerkleTree(reconstructedLeaves) { blake3(it) }
        val treeLayers = reconstructedTree.getHexLayers()
        val reconstructedAppHash = reconstructedTree.getRoot().toHex()

        val identityProofRoot = identityProof.calculateRoot(listOf(1), listOf(identityLeaf), 6).toHex()
        val keysProofRoot = keysProof.calculateRoot(listOf(2), listOf(publicKeysLeaf), 6).toHex()
        val contractsProofRoot = contractsProof.calculateRoot(listOf(3), listOf(contractsLeaf), 6).toHex()
        val documentsProofRoot = documentsProof.calculateRoot(listOf(4), listOf(documentsLeaf), 6).toHex()
        val identitiesIdsProofRoot = identitiesByPublicKeyHashesProof.calculateRoot(listOf(1, 2), listOf(identityLeaf, publicKeysLeaf), 6).toHex()

        assertEquals(
            identityProof.getHexProofHashes(),
            listOf(
                treeLayers[0][0],
                treeLayers[1][1],
                treeLayers[1][2]
            )
        )

        assertEquals(
            keysProof.getHexProofHashes(),
            listOf(
                treeLayers[0][3],
                treeLayers[1][0],
                treeLayers[1][2]
            )
        )

        assertEquals(
            contractsProof.getHexProofHashes(),
            listOf(
                treeLayers[0][2],
                treeLayers[1][0],
                treeLayers[1][2]
            )
        )

        assertEquals(
            documentsProof.getHexProofHashes(),
            listOf(
                treeLayers[0][5],
                treeLayers[2][0]
            )
        )

        assertEquals(
            identitiesByPublicKeyHashesProof.getHexProofHashes(),
            listOf(
                treeLayers[0][0],
                treeLayers[0][3],
                treeLayers[1][2]
            )
        )

        assertEquals(identityProofRoot, reconstructedAppHash)
        assertEquals(keysProofRoot, reconstructedAppHash)
        assertEquals(contractsProofRoot, reconstructedAppHash)
        assertEquals(documentsProofRoot, reconstructedAppHash)
        assertEquals(identitiesIdsProofRoot, reconstructedAppHash)
    }
}
