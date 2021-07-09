package org.dashj.platform.dapiclient.model

import org.dashj.bls.BLS
import org.dashj.merk.MerkVerifyProof
import org.dashj.platform.dapiclient.StateRepositoryMock
import org.dashj.platform.dpp.identity.IdentityCreateTransition
import org.dashj.platform.dpp.statetransition.StateTransitionFactory
import org.dashj.platform.dpp.statetransition.StateTransitionIdentitySigned
import org.dashj.platform.dpp.util.HashUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

object VerifyProofTest {

    val factory = StateTransitionFactory(StateRepositoryMock())

    @BeforeAll
    @JvmStatic
    fun startUp() {
        MerkVerifyProof.init()
    }
    /*

    identity creation:

2021-06-01 10:16:52.080 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] broadcastStateTransitionAndWait: success (1.0): Proof(rootTreeProof=[1, 0, 0, 0, 3, 49, -42, 96, 73, -126, 4, 27, 73, 35, 74, -69, 13, 61, -55, 34, -25, -103, -30, -104, 97, 83, 31, -51, -7, -101, 68, 89, -106, 58, -123, -75, -66, 49, 21, -74, -128, 89, -9, -111, 88, -45, 23, -102, -30, 16, -80, 65, 120, 65, -52, 113, -22, 21, 61, 56, 97, 79, -17, 80, -11, 1, 3], storeTreeProof=[1, 79, -75, -112, -88, 92, -84, -128, 108, 50, 5, 68, 86, -18, 51, -21, -63, 0, 21, 69, 86, 2, -69, -1, -29, 85, -78, 101, -88, 87, 37, -72, -19, -36, -92, -48, -126, 60, 34, -90, 37, 1, 16, 3, 32, 17, 12, 78, -36, -113, 100, -32, 51, -46, 104, 44, 23, 120, 18, -54, 56, -105, 37, -65, 13, -71, -50, -70, -40, -102, -97, 108, 0, 89, 91, 104, 75, -115, 0, -91, 98, 105, 100, 88, 32, 17, 12, 78, -36, -113, 100, -32, 51, -46, 104, 44, 23, 120, 18, -54, 56, -105, 37, -65, 13, -71, -50, -70, -40, -102, -97, 108, 0, 89, 91, 104, 75, 103, 98, 97, 108, 97, 110, 99, 101, 26, 59, -102, -57, -25, 104, 114, 101, 118, 105, 115, 105, 111, 110, 0, 106, 112, 117, 98, 108, 105, 99, 75, 101, 121, 115, -127, -93, 98, 105, 100, 0, 100, 100, 97, 116, 97, 88, 33, 2, -45, -44, -42, -101, -60, -86, -52, 103, 57, -64, -61, 36, 49, -68, -95, -19, -91, 96, 105, 91, -51, 87, 19, 127, 27, -79, 35, -71, -57, -13, 38, 100, 100, 116, 121, 112, 101, 0, 111, 112, 114, 111, 116, 111, 99, 111, 108, 86, 101, 114, 115, 105, 111, 110, 0, 2, -111, -38, -9, 121, -99, 105, -6, 112, 39, 76, -105, -52, 112, -12, 65, 20, -98, 24, 34, -86, 16, 2, 65, 19, -57, 125, 52, -41, -22, 3, -31, -44, 126, 58, 12, -110, -80, -52, -103, -66, -110, -23, 16, 1, 95, -70, -23, 55, -68, -123, -64, -62, 107, -73, 110, -10, -42, 4, -4, -72, -50, -75, 80, -122, 17, 17, 2, -55, -104, 85, -6, -93, -112, 80, 13, 7, -64, 4, -64, 1, -60, -76, 97, 73, -27, -3, 4, 16, 1, 73, -126, -9, 112, -24, 87, -28, 102, 66, 107, 16, -86, 95, -51, -45, 124, 15, -81, 101, -57, 17, 2, -111, 91, 45, 25, 14, 116, 78, -53, -56, -85, 42, -20, 111, 118, 82, 107, -112, -62, 100, -104, 16, 1, -89, 45, 75, 67, 123, 27, -38, 48, -120, 93, -32, -76, 6, -3, -122, -105, -81, -37, -86, 38, 17, 2, -126, 82, -59, 120, 61, -125, 114, 116, -39, -1, -78, 66, 123, 56, 99, 21, 67, -49, -105, -34, 16, 1, 44, 116, 9, 55, 64, 11, -27, 75, -49, 97, -120, 9, 106, -72, -19, 43, -64, 72, -47, 63, 17, 2, 108, 64, 123, -15, 4, 44, -5, -3, 67, 99, -38, 94, -95, 40, 91, 58, 77, -1, -101, -12, 16, 1, 91, 109, -21, 107, 44, -23, 48, 107, 59, 20, 55, -67, -79, -96, -113, -63, -46, 118, -74, 70, 17])
2021-06-01 10:16:52.090 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] proof: 014fb590a85cac806c32054456ee33ebc10015455602bbffe355b265a85725b8eddca4d0823c22a62501100320110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b8d00a56269645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b6762616c616e63651a3b9ac7e7687265766973696f6e006a7075626c69634b65797381a3626964006464617461582102d3d4d69bc4aacc6739c0c32431bca1eda560695bcd57137f1bb123b9c7f326646474797065006f70726f746f636f6c56657273696f6e000291daf7799d69fa70274c97cc70f441149e1822aa10024113c77d34d7ea03e1d47e3a0c92b0cc99be92e910015fbae937bc85c0c26bb76ef6d604fcb8ceb55086111102c99855faa390500d07c004c001c4b46149e5fd0410014982f770e857e466426b10aa5fcdd37c0faf65c71102915b2d190e744ecbc8ab2aec6f76526b90c264981001a72d4b437b1bda30885de0b406fd8697afdbaa2611028252c5783d837274d9ffb2427b38631543cf97de10012c740937400be54bcf6188096ab8ed2bc048d13f11026c407bf1042cfbfd4363da5ea1285b3a4dff9bf410015b6deb6b2ce9306b3b1437bdb1a08fc1d276b64611
2021-06-01 10:16:52.096 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] state transition: a5647479706502697369676e61747572655841204dc0dfaa7c7757f54374f3adce1efd9fca807de88625fc1d51d3449618b1d8201c1b1a9c32c223343cfa822975734a6f75c5dfee3d9cf136f4b30f5ed1c776f26a7075626c69634b65797381a3626964006464617461582102d3d4d69bc4aacc6739c0c32431bca1eda560695bcd57137f1bb123b9c7f326646474797065006e61737365744c6f636b50726f6f66a46474797065006b696e7374616e744c6f636b58a501950f25fbb056c5f6a17493c2dbe6b24fec1790c78de1d1f117642bed6667c6df0000000047a2e6cf5774f1cc1b577c3f9734b2ace0919b4aea08aab1adcb371af798baf480cc6aa57df4b90386fa022d9de9837e5eed4025b56c4ce9f2972a98294386f8309284acaaa362c696adf339c15473ac01bf7efb2421f62704e0a19dc60ba95a50a446c71976a83f30ff18a6439b66864f84dfab04b21e5b527e21ce196065a26b6f7574707574496e646578016b7472616e73616374696f6e58de0100000001950f25fbb056c5f6a17493c2dbe6b24fec1790c78de1d1f117642bed6667c6df000000006a4730440220266b5e01c097d2391e98692afa58309aea13ec95e487dc4e480ee47a8e3e6e73022073d1ae9981496dcbe4a1f01543f4e1f982b6d866ae7997e2269195024d0cd81d012103db8bb5d09750e15b2770bc157f5456af1de2ee19199a1155ea1a3f219f6fd30effffffff0230910300000000001976a914b75e475b4cdca816df03a37577af7a9a0f61d2fe88ac40420f0000000000166a143f1b3daea6d15b6ea229eeed6aaf254439c2af6d000000006f70726f746f636f6c56657273696f6e00
2021-06-01 10:16:52.098 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:16:52.099 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      4fb590a85cac806c32054456ee33ebc100154556
2021-06-01 10:16:52.099 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:16:52.099 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        bbffe355b265a85725b8eddca4d0823c22a62501
2021-06-01 10:16:52.099 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:16:52.102 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KV(key, value)) => 0x03 <1-byte key length> <n-byte key> <2-byte value length> <n-byte value>
2021-06-01 10:16:52.103 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      =>      32 110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b / 29YpLarhpLKdFQWahhDK4s1VaS9jcYXHVGQpYKZ235ZU
2021-06-01 10:16:52.104 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      =>      141 a56269645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b6762616c616e63651a3b9ac7e7687265766973696f6e006a7075626c69634b65797381a3626964006464617461582102d3d4d69bc4aacc6739c0c32431bca1eda560695bcd57137f1bb123b9c7f326646474797065006f70726f746f636f6c56657273696f6e00
2021-06-01 10:16:52.106 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      => {publicKeys=[{data=[B@644785e, id=0, type=0}], balance=999999463, protocolVersion=0, id=[B@e88143f, revision=0}
2021-06-01 10:16:52.106 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:16:52.106 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        91daf7799d69fa70274c97cc70f441149e1822aa
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        4113c77d34d7ea03e1d47e3a0c92b0cc99be92e9
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      5fbae937bc85c0c26bb76ef6d604fcb8ceb55086
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:16:52.107 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        c99855faa390500d07c004c001c4b46149e5fd04
2021-06-01 10:16:52.108 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:16:52.108 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:16:52.108 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      4982f770e857e466426b10aa5fcdd37c0faf65c7
2021-06-01 10:16:52.108 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:16:52.108 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:16:52.110 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        915b2d190e744ecbc8ab2aec6f76526b90c26498
2021-06-01 10:16:52.110 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:16:52.110 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      a72d4b437b1bda30885de0b406fd8697afdbaa26
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        8252c5783d837274d9ffb2427b38631543cf97de
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      2c740937400be54bcf6188096ab8ed2bc048d13f
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:16:52.111 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:16:52.112 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        6c407bf1042cfbfd4363da5ea1285b3a4dff9bf4
2021-06-01 10:16:52.112 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:16:52.112 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:16:52.112 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      5b6deb6b2ce9306b3b1437bdb1a08fc1d276b646
2021-06-01 10:16:52.112 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
    preorder:

2021-06-01 10:17:03.618 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] broadcastStateTransitionAndWait: success (1.0): Proof(rootTreeProof=[1, 0, 0, 0, 2, 2, -107, -98, 89, -44, -24, -27, 52, 109, -126, 67, -88, -92, -19, 1, 52, 81, 97, 29, -126, -75, -75, -49, 39, 52, -22, -64, 88, 27, -82, -45, 43, 19, -18, -20, 28, 2, 37, 54, 58, 1, 2], storeTreeProof=[1, -62, -43, -15, -55, -91, 75, -13, 53, -105, -110, -24, -59, 40, -71, 92, 101, 58, 78, 126, 94, 2, -31, 2, 106, 121, -42, 13, 12, 84, -92, -30, 56, 65, -86, -34, 26, 92, 119, 51, 25, 45, 16, 1, -124, -13, 18, -55, 1, -20, 78, -68, 25, 94, -36, -45, 116, 19, -23, -102, 44, -7, 118, -60, 2, -36, 122, 102, -76, 79, -34, -16, -122, 73, -119, -47, -51, 94, -42, -18, -17, 33, -39, -55, 26, 16, 3, 32, 49, -7, 83, 22, -85, 70, 27, -119, -34, -125, -38, 107, 81, 122, 35, -72, -60, 46, 23, -75, 83, 109, -50, 57, -101, 112, 39, -60, 86, 29, -7, 80, -29, 0, -89, 99, 36, 105, 100, 88, 32, 49, -7, 83, 22, -85, 70, 27, -119, -34, -125, -38, 107, 81, 122, 35, -72, -60, 46, 23, -75, 83, 109, -50, 57, -101, 112, 39, -60, 86, 29, -7, 80, 101, 36, 116, 121, 112, 101, 104, 112, 114, 101, 111, 114, 100, 101, 114, 104, 36, 111, 119, 110, 101, 114, 73, 100, 88, 32, 17, 12, 78, -36, -113, 100, -32, 51, -46, 104, 44, 23, 120, 18, -54, 56, -105, 37, -65, 13, -71, -50, -70, -40, -102, -97, 108, 0, 89, 91, 104, 75, 105, 36, 114, 101, 118, 105, 115, 105, 111, 110, 1, 111, 36, 100, 97, 116, 97, 67, 111, 110, 116, 114, 97, 99, 116, 73, 100, 88, 32, -83, -124, 100, 32, 4, 90, 65, 82, 47, -122, -113, -100, 13, 94, 126, 94, -81, -39, 96, -24, -13, 74, 4, 88, -117, 115, 88, 61, 6, -94, -128, -56, 112, 36, 112, 114, 111, 116, 111, 99, 111, 108, 86, 101, 114, 115, 105, 111, 110, 0, 112, 115, 97, 108, 116, 101, 100, 68, 111, 109, 97, 105, 110, 72, 97, 115, 104, 88, 32, -82, -17, 22, 73, 102, 37, -103, 77, -60, 125, 91, 64, -76, 100, -80, -104, 78, -91, 111, -38, -62, 100, 28, 10, 111, 8, 35, -12, -105, 49, -42, 33, 17, 2, 15, -120, 18, 127, 45, 106, -6, 42, 83, -82, -51, 96, 11, 119, -42, 64, 25, -76, 8, -13, 16, 1, 68, 3, 86, -58, -42, -42, -65, 12, -118, 2, -68, -54, -21, -91, -122, 118, -118, 32, 124, -24, 17, 2, -40, -45, 124, -8, 58, 21, -88, -68, 76, 87, -75, 15, -127, -41, -90, -120, -12, 78, -74, -10, 16, 1, -24, -24, 23, -3, 5, 100, 42, 34, 118, -59, 2, 106, -86, 34, -74, -46, -40, 41, 0, -120, 17, 2, -105, -108, -20, -75, 73, 60, 27, -122, 33, -23, -118, 85, 92, -60, -60, 53, 44, 126, 59, 28, 16, 1, 63, 83, 119, -84, 11, 97, 96, 8, 63, -47, 60, -17, 114, 97, -110, -51, 7, -65, -127, -10, 17, 2, -87, -5, 38, -81, -90, 53, -102, -65, -111, -102, 14, -68, 36, 65, -24, 92, -41, -125, 92, 120, 16, 1, -121, -106, -89, 39, -21, 76, 63, 50, 57, 35, -118, -6, -23, -88, -81, -40, 122, -116, -13, -43, 17, 17, 2, -33, -85, 96, 65, 69, -19, 102, -51, -16, -91, 73, 51, -100, 56, 113, 57, -73, 121, -35, 32, 16, 1, 112, -28, -95, 29, 5, -54, 94, 5, -20, 108, -96, -8, -119, -33, 93, -39, 84, -58, -35, 70, 17, 2, -78, -127, 9, -31, -32, 34, 6, 12, 80, 1, -116, 93, -25, 109, 73, -62, 117, -92, 4, -45, 16, 1, -119, -124, 42, -52, -102, -50, -37, -31, -68, -49, -123, -63, -20, 70, -46, 109, 93, 52, -118, -30, 17])
2021-06-01 10:17:03.625 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] proof: 01c2d5f1c9a54bf3359792e8c528b95c653a4e7e5e02e1026a79d60d0c54a4e23841aade1a5c7733192d100184f312c901ec4ebc195edcd37413e99a2cf976c402dc7a66b44fdef0864989d1cd5ed6eeef21d9c91a10032031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950e300a763246964582031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950652474797065687072656f7264657268246f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b69247265766973696f6e016f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c8702470726f746f636f6c56657273696f6e007073616c746564446f6d61696e486173685820aeef16496625994dc47d5b40b464b0984ea56fdac2641c0a6f0823f49731d62111020f88127f2d6afa2a53aecd600b77d64019b408f31001440356c6d6d6bf0c8a02bccaeba586768a207ce81102d8d37cf83a15a8bc4c57b50f81d7a688f44eb6f61001e8e817fd05642a2276c5026aaa22b6d2d829008811029794ecb5493c1b8621e98a555cc4c4352c7e3b1c10013f5377ac0b6160083fd13cef726192cd07bf81f61102a9fb26afa6359abf919a0ebc2441e85cd7835c7810018796a727eb4c3f3239238afae9a8afd87a8cf3d5111102dfab604145ed66cdf0a549339c387139b779dd20100170e4a11d05ca5e05ec6ca0f889df5dd954c6dd461102b28109e1e022060c50018c5de76d49c275a404d3100189842acc9acedbe1bccf85c1ec46d26d5d348ae211
2021-06-01 10:17:03.630 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] state transition: a6647479706501676f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b697369676e617475726558412090e39108c1ae99e8e054c4e93f86a4af925cb861016eedaae45177ca09fecd1b72c68a2f5ba29697c8ed464ac552db97cb6c37d08091f53d01eeeafaa92126726b7472616e736974696f6e7381a663246964582031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950652474797065687072656f726465726724616374696f6e006824656e74726f70795820853bd951419936a6103630f5dbcb22218124b4e2ceb05607b1ee999aadd15d996f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c87073616c746564446f6d61696e486173685820aeef16496625994dc47d5b40b464b0984ea56fdac2641c0a6f0823f49731d6216f70726f746f636f6c56657273696f6e00747369676e61747572655075626c69634b6579496400
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      c2d5f1c9a54bf3359792e8c528b95c653a4e7e5e
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        e1026a79d60d0c54a4e23841aade1a5c7733192d
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      84f312c901ec4ebc195edcd37413e99a2cf976c4
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        dc7a66b44fdef0864989d1cd5ed6eeef21d9c91a
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:03.631 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KV(key, value)) => 0x03 <1-byte key length> <n-byte key> <2-byte value length> <n-byte value>
2021-06-01 10:17:03.632 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      =>      32 31f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950 / 4N5UuREBFqpsYTGXZP35PY6SqKAEJiivxdxwnAe9iytT
2021-06-01 10:17:03.634 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      =>      227 a763246964582031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950652474797065687072656f7264657268246f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b69247265766973696f6e016f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c8702470726f746f636f6c56657273696f6e007073616c746564446f6d61696e486173685820aeef16496625994dc47d5b40b464b0984ea56fdac2641c0a6f0823f49731d621
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      => {$protocolVersion=0, $ownerId=[B@1542406, saltedDomainHash=[B@6715ac7, $revision=1, $dataContractId=[B@4d678f4, $id=[B@8e4081d, $type=preorder}
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        0f88127f2d6afa2a53aecd600b77d64019b408f3
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      440356c6d6d6bf0c8a02bccaeba586768a207ce8
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        d8d37cf83a15a8bc4c57b50f81d7a688f44eb6f6
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      e8e817fd05642a2276c5026aaa22b6d2d8290088
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:03.635 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        9794ecb5493c1b8621e98a555cc4c4352c7e3b1c
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      3f5377ac0b6160083fd13cef726192cd07bf81f6
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        a9fb26afa6359abf919a0ebc2441e85cd7835c78
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      8796a727eb4c3f3239238afae9a8afd87a8cf3d5
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        dfab604145ed66cdf0a549339c387139b779dd20
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:03.636 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:03.637 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      70e4a11d05ca5e05ec6ca0f889df5dd954c6dd46
2021-06-01 10:17:03.637 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:03.637 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:03.637 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        b28109e1e022060c50018c5de76d49c275a404d3
2021-06-01 10:17:03.637 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:03.637 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:03.637 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      89842acc9acedbe1bccf85c1ec46d26d5d348ae2
2021-06-01 10:17:03.637 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
    domain:

2021-06-01 10:17:10.145 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] broadcastStateTransitionAndWait: success (1.0): Proof(rootTreeProof=[1, 0, 0, 0, 2, 2, -107, -98, 89, -44, -24, -27, 52, 109, -126, 67, -88, -92, -19, 1, 52, 81, 97, 29, -126, -75, -75, -49, 39, 52, -22, -64, 88, 27, -82, -45, 43, 19, -18, -20, 28, 2, 37, 54, 58, 1, 2], storeTreeProof=[1, -62, -43, -15, -55, -91, 75, -13, 53, -105, -110, -24, -59, 40, -71, 92, 101, 58, 78, 126, 94, 2, -31, 2, 106, 121, -42, 13, 12, 84, -92, -30, 56, 65, -86, -34, 26, 92, 119, 51, 25, 45, 16, 1, -118, 83, 64, -6, 60, 87, -115, 85, 79, 27, 112, 43, -67, 47, 30, -20, -105, -98, 112, 32, 2, -105, -108, -20, -75, 73, 60, 27, -122, 33, -23, -118, 85, 92, -60, -60, 53, 44, 126, 59, 28, 16, 1, -91, -46, 78, 75, 70, 47, -112, 120, -85, -110, 71, 95, 25, 123, -61, 24, -117, -20, -84, -88, 2, -119, 55, -20, -30, -32, 6, -101, -79, -45, 35, -100, -73, 121, -46, -127, -71, -92, -96, 23, 35, 16, 2, 93, -40, 4, 19, -125, -85, 40, 24, -54, 92, 14, 110, -121, 92, -28, -21, -20, 104, -72, 4, 3, 32, 64, 11, 49, -65, 114, -28, 38, 2, -30, 78, 120, 105, 47, -31, 85, 2, -106, -10, -5, 84, -58, 8, -52, 83, 96, -20, -67, -17, 30, 96, 120, 50, -119, 1, -84, 99, 36, 105, 100, 88, 32, 64, 11, 49, -65, 114, -28, 38, 2, -30, 78, 120, 105, 47, -31, 85, 2, -106, -10, -5, 84, -58, 8, -52, 83, 96, -20, -67, -17, 30, 96, 120, 50, 101, 36, 116, 121, 112, 101, 102, 100, 111, 109, 97, 105, 110, 101, 108, 97, 98, 101, 108, 105, 120, 45, 104, 97, 115, 104, 45, 50, 53, 103, 114, 101, 99, 111, 114, 100, 115, -95, 116, 100, 97, 115, 104, 85, 110, 105, 113, 117, 101, 73, 100, 101, 110, 116, 105, 116, 121, 73, 100, 88, 32, 17, 12, 78, -36, -113, 100, -32, 51, -46, 104, 44, 23, 120, 18, -54, 56, -105, 37, -65, 13, -71, -50, -70, -40, -102, -97, 108, 0, 89, 91, 104, 75, 104, 36, 111, 119, 110, 101, 114, 73, 100, 88, 32, 17, 12, 78, -36, -113, 100, -32, 51, -46, 104, 44, 23, 120, 18, -54, 56, -105, 37, -65, 13, -71, -50, -70, -40, -102, -97, 108, 0, 89, 91, 104, 75, 105, 36, 114, 101, 118, 105, 115, 105, 111, 110, 1, 108, 112, 114, 101, 111, 114, 100, 101, 114, 83, 97, 108, 116, 88, 32, 43, 65, -22, -62, -19, 102, 113, 114, -119, 31, -123, -5, 79, -117, 102, -106, -105, 57, 82, -101, 54, 103, -124, 11, 28, 15, -12, -5, -96, 118, -35, -71, 110, 115, 117, 98, 100, 111, 109, 97, 105, 110, 82, 117, 108, 101, 115, -95, 111, 97, 108, 108, 111, 119, 83, 117, 98, 100, 111, 109, 97, 105, 110, 115, -12, 111, 36, 100, 97, 116, 97, 67, 111, 110, 116, 114, 97, 99, 116, 73, 100, 88, 32, -83, -124, 100, 32, 4, 90, 65, 82, 47, -122, -113, -100, 13, 94, 126, 94, -81, -39, 96, -24, -13, 74, 4, 88, -117, 115, 88, 61, 6, -94, -128, -56, 111, 110, 111, 114, 109, 97, 108, 105, 122, 101, 100, 76, 97, 98, 101, 108, 105, 120, 45, 104, 97, 115, 104, 45, 50, 53, 112, 36, 112, 114, 111, 116, 111, 99, 111, 108, 86, 101, 114, 115, 105, 111, 110, 0, 120, 26, 110, 111, 114, 109, 97, 108, 105, 122, 101, 100, 80, 97, 114, 101, 110, 116, 68, 111, 109, 97, 105, 110, 78, 97, 109, 101, 100, 100, 97, 115, 104, 17, 17, 17, 2, -87, -5, 38, -81, -90, 53, -102, -65, -111, -102, 14, -68, 36, 65, -24, 92, -41, -125, 92, 120, 16, 1, -121, -106, -89, 39, -21, 76, 63, 50, 57, 35, -118, -6, -23, -88, -81, -40, 122, -116, -13, -43, 17, 17, 2, -33, -85, 96, 65, 69, -19, 102, -51, -16, -91, 73, 51, -100, 56, 113, 57, -73, 121, -35, 32, 16, 1, 112, -28, -95, 29, 5, -54, 94, 5, -20, 108, -96, -8, -119, -33, 93, -39, 84, -58, -35, 70, 17, 2, -78, -127, 9, -31, -32, 34, 6, 12, 80, 1, -116, 93, -25, 109, 73, -62, 117, -92, 4, -45, 16, 1, -119, -124, 42, -52, -102, -50, -37, -31, -68, -49, -123, -63, -20, 70, -46, 109, 93, 52, -118, -30, 17])
2021-06-01 10:17:10.153 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] proof: 01c2d5f1c9a54bf3359792e8c528b95c653a4e7e5e02e1026a79d60d0c54a4e23841aade1a5c7733192d10018a5340fa3c578d554f1b702bbd2f1eec979e7020029794ecb5493c1b8621e98a555cc4c4352c7e3b1c1001a5d24e4b462f9078ab92475f197bc3188becaca8028937ece2e0069bb1d3239cb779d281b9a4a0172310025dd8041383ab2818ca5c0e6e875ce4ebec68b8040320400b31bf72e42602e24e78692fe1550296f6fb54c608cc5360ecbdef1e6078328901ac632469645820400b31bf72e42602e24e78692fe1550296f6fb54c608cc5360ecbdef1e60783265247479706566646f6d61696e656c6162656c69782d686173682d3235677265636f726473a17464617368556e697175654964656e7469747949645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b68246f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b69247265766973696f6e016c7072656f7264657253616c7458202b41eac2ed667172891f85fb4f8b66969739529b3667840b1c0ff4fba076ddb96e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c86f6e6f726d616c697a65644c6162656c69782d686173682d3235702470726f746f636f6c56657273696f6e00781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d65646461736811111102a9fb26afa6359abf919a0ebc2441e85cd7835c7810018796a727eb4c3f3239238afae9a8afd87a8cf3d5111102dfab604145ed66cdf0a549339c387139b779dd20100170e4a11d05ca5e05ec6ca0f889df5dd954c6dd461102b28109e1e022060c50018c5de76d49c275a404d3100189842acc9acedbe1bccf85c1ec46d26d5d348ae211
2021-06-01 10:17:10.162 12441-12552/hashengineering.darkcoin.wallet.devnet I/DapiClient: [DefaultDispatcher-worker-1] state transition: a6647479706501676f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b697369676e6174757265584120e6306016070cd4a7fce21770dfa91fd7dc83899b4028d9c7fae384367a1a0e82301835d7e1410786936d067ca3128c077ecf5f4b517360b5230b08d2016335d06b7472616e736974696f6e7381ab632469645820400b31bf72e42602e24e78692fe1550296f6fb54c608cc5360ecbdef1e60783265247479706566646f6d61696e656c6162656c69782d686173682d32356724616374696f6e00677265636f726473a17464617368556e697175654964656e7469747949645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b6824656e74726f707958205a3e51d4485e3b09792ba6cff357b0fb6827b82acca1f49f5f52600906f101dc6c7072656f7264657253616c7458202b41eac2ed667172891f85fb4f8b66969739529b3667840b1c0ff4fba076ddb96e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c86f6e6f726d616c697a65644c6162656c69782d686173682d3235781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d6564646173686f70726f746f636f6c56657273696f6e00747369676e61747572655075626c69634b6579496400
2021-06-01 10:17:10.162 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:10.162 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      c2d5f1c9a54bf3359792e8c528b95c653a4e7e5e
2021-06-01 10:17:10.162 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        e1026a79d60d0c54a4e23841aade1a5c7733192d
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      8a5340fa3c578d554f1b702bbd2f1eec979e7020
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        9794ecb5493c1b8621e98a555cc4c4352c7e3b1c
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      a5d24e4b462f9078ab92475f197bc3188becaca8
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        8937ece2e0069bb1d3239cb779d281b9a4a01723
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:10.163 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:10.164 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        5dd8041383ab2818ca5c0e6e875ce4ebec68b804
2021-06-01 10:17:10.164 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KV(key, value)) => 0x03 <1-byte key length> <n-byte key> <2-byte value length> <n-byte value>
2021-06-01 10:17:10.164 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      =>      32 400b31bf72e42602e24e78692fe1550296f6fb54c608cc5360ecbdef1e607832 / 5JzzbNgE5Jum2to5CEeYMWeg21RSirThyThoH6QomRVT
2021-06-01 10:17:10.166 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      =>      393 ac632469645820400b31bf72e42602e24e78692fe1550296f6fb54c608cc5360ecbdef1e60783265247479706566646f6d61696e656c6162656c69782d686173682d3235677265636f726473a17464617368556e697175654964656e7469747949645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b68246f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b69247265766973696f6e016c7072656f7264657253616c7458202b41eac2ed667172891f85fb4f8b66969739529b3667840b1c0ff4fba076ddb96e737562646f6d61696e52756c6573a16f616c6c6f77537562646f6d61696e73f46f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c86f6e6f726d616c697a65644c6162656c69782d686173682d3235702470726f746f636f6c56657273696f6e00781a6e6f726d616c697a6564506172656e74446f6d61696e4e616d656464617368
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                      => {preorderSalt=[B@ff03a8b, $protocolVersion=0, normalizedParentDomainName=dash, normalizedLabel=x-hash-25, records={dashUniqueIdentityId=[B@da17768}, $ownerId=[B@291b781, subdomainRules={allowSubdomains=false}, label=x-hash-25, $revision=1, $dataContractId=[B@cbaa026, $id=[B@ba5f467, $type=domain}
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/chatty: uid=10157(hashengineering.darkcoin.wallet.devnet) DefaultDispatch identical 1 line
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        a9fb26afa6359abf919a0ebc2441e85cd7835c78
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      8796a727eb4c3f3239238afae9a8afd87a8cf3d5
2021-06-01 10:17:10.168 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        dfab604145ed66cdf0a549339c387139b779dd20
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      70e4a11d05ca5e05ec6ca0f889df5dd954c6dd46
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(KVHash(hash)) => 0x02 <20-byte hash>
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>        b28109e1e022060c50018c5de76d49c275a404d3
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Parent => 0x10
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Push(Hash(hash)) => 0x01 <20-byte hash>
2021-06-01 10:17:10.169 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out:                  =>      89842acc9acedbe1bccf85c1ec46d26d5d348ae2
2021-06-01 10:17:10.170 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: Child => 0x11
2021-06-01 10:17:10.170 12441-12552/hashengineering.darkcoin.wallet.devnet I/System.out: c2d5f1c9a54bf3359792e8c528b95c653a4e7e5e

     */
    @Test
    fun verifyProofTest() {
        val proof = HashUtils.fromHex(
            "012ab320137f11da08ef98e248921c07aa8835fd48023fefb99403535bdc071867e015b51b6f44d883211001ba50e2d157009f0b459e2f47820cf18cd8d7759a023d2713a3445d43f88a9fd633f17e17290c29fcfe1001973a704fed3ae3cd362f67bce9a0a0f0e14d753b03208f4aa25b6ed8236d3186a4b90c1e8c0507072b102a5796ac5678755a05fff496fc00a96324696458208f4aa25b6ed8236d3186a4b90c1e8c0507072b102a5796ac5678755a05fff4966524747970656770726f66696c6568246f776e6572496458204fbf7a993ef837672f42f2993879bb814444dcd8390834d94094d5072dc9bf7d69247265766973696f6e18216a246372656174656441741b00000179477757c96a247570646174656441741b00000179c806abe76b646973706c61794e616d657758616e646572204861727269732d3633303230333839316f2464617461436f6e747261637449645820584bac6a1b6a9dc2b2cfc24a8bf4b2e0ba2fa7a496d1e9488aaba75e4835a844702470726f746f636f6c56657273696f6e001001d5b6a50df7eb1a1c02b6d09eb8b0e1c5f1bafc541102fdb592ea3d5a8626b86f9d6f5ad83b63736972001001689d6bc3d5b4f6ae8b5b86d4b1fadab5d594926c1102af87719bd5cf7f0702fd116ae1412761bb4126e710012c29edf9868c2c38375f95ee76f9a91831aa41cf1111023bab75744254b2f71a838a52ed64c5ca5fc26d231001fd511e32e9467f87495de3e32f022f12c363acbd1111"
        )
        val stateTransition =
            HashUtils.fromHex("a6647479706501676f776e6572496458204fbf7a993ef837672f42f2993879bb814444dcd8390834d94094d5072dc9bf7d697369676e61747572655841203e33e7e14a54fd484c3c1b5b484750371d2404ad8030fdd211eb8a0c8b5b4b0e522c5439b9e4da713e2843a6f49abb9d36afdba3eb56be78cfc847d96be64f1e6b7472616e736974696f6e7381a76324696458208f4aa25b6ed8236d3186a4b90c1e8c0507072b102a5796ac5678755a05fff4966524747970656770726f66696c656724616374696f6e0169247265766973696f6e18216a247570646174656441741b00000179c806abe76b646973706c61794e616d657758616e646572204861727269732d3633303230333839316f2464617461436f6e747261637449645820584bac6a1b6a9dc2b2cfc24a8bf4b2e0ba2fa7a496d1e9488aaba75e4835a8446f70726f746f636f6c56657273696f6e00747369676e61747572655075626c69634b6579496400")

        val verifyProof =
            DefaultVerifyProof(factory.createFromBuffer(stateTransition) as StateTransitionIdentitySigned).verify(
                Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
            )
        assertTrue(verifyProof)
    }

    @Test
    fun verifyProofIdentityCreateTest() {
        BLS.Init()
        val proof = HashUtils.fromHex(
            "014fb590a85cac806c32054456ee33ebc10015455602bbffe355b265a85725b8eddca4d0823c22a62501100320110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b8d00a56269645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b6762616c616e63651a3b9ac7e7687265766973696f6e006a7075626c69634b65797381a3626964006464617461582102d3d4d69bc4aacc6739c0c32431bca1eda560695bcd57137f1bb123b9c7f326646474797065006f70726f746f636f6c56657273696f6e000291daf7799d69fa70274c97cc70f441149e1822aa10024113c77d34d7ea03e1d47e3a0c92b0cc99be92e910015fbae937bc85c0c26bb76ef6d604fcb8ceb55086111102c99855faa390500d07c004c001c4b46149e5fd0410014982f770e857e466426b10aa5fcdd37c0faf65c71102915b2d190e744ecbc8ab2aec6f76526b90c264981001a72d4b437b1bda30885de0b406fd8697afdbaa2611028252c5783d837274d9ffb2427b38631543cf97de10012c740937400be54bcf6188096ab8ed2bc048d13f11026c407bf1042cfbfd4363da5ea1285b3a4dff9bf410015b6deb6b2ce9306b3b1437bdb1a08fc1d276b64611"
        )
        val stateTransition =
            HashUtils.fromHex("a5647479706502697369676e61747572655841204dc0dfaa7c7757f54374f3adce1efd9fca807de88625fc1d51d3449618b1d8201c1b1a9c32c223343cfa822975734a6f75c5dfee3d9cf136f4b30f5ed1c776f26a7075626c69634b65797381a3626964006464617461582102d3d4d69bc4aacc6739c0c32431bca1eda560695bcd57137f1bb123b9c7f326646474797065006e61737365744c6f636b50726f6f66a46474797065006b696e7374616e744c6f636b58a501950f25fbb056c5f6a17493c2dbe6b24fec1790c78de1d1f117642bed6667c6df0000000047a2e6cf5774f1cc1b577c3f9734b2ace0919b4aea08aab1adcb371af798baf480cc6aa57df4b90386fa022d9de9837e5eed4025b56c4ce9f2972a98294386f8309284acaaa362c696adf339c15473ac01bf7efb2421f62704e0a19dc60ba95a50a446c71976a83f30ff18a6439b66864f84dfab04b21e5b527e21ce196065a26b6f7574707574496e646578016b7472616e73616374696f6e58de0100000001950f25fbb056c5f6a17493c2dbe6b24fec1790c78de1d1f117642bed6667c6df000000006a4730440220266b5e01c097d2391e98692afa58309aea13ec95e487dc4e480ee47a8e3e6e73022073d1ae9981496dcbe4a1f01543f4e1f982b6d866ae7997e2269195024d0cd81d012103db8bb5d09750e15b2770bc157f5456af1de2ee19199a1155ea1a3f219f6fd30effffffff0230910300000000001976a914b75e475b4cdca816df03a37577af7a9a0f61d2fe88ac40420f0000000000166a143f1b3daea6d15b6ea229eeed6aaf254439c2af6d000000006f70726f746f636f6c56657273696f6e00")

        val transition = factory.createFromBuffer(stateTransition)

        val verifyProof =
            DefaultVerifyProof(transition as IdentityCreateTransition).verify(
                Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
            )

        assertTrue(verifyProof)

        val expectedHash = HashUtils.fromHex("49b0810134e560d1e16b56998a9145dd3ad50e6c")
        val verifyProofMerk = MerkLibVerifyProof(transition as StateTransitionIdentitySigned, expectedHash).verify(
            Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
        )
        assertTrue(verifyProofMerk)
    }

    @Test
    fun verifyProofDocumentCreateTest() {
        MerkVerifyProof.init()
        val proof = HashUtils.fromHex(
            "01c2d5f1c9a54bf3359792e8c528b95c653a4e7e5e02e1026a79d60d0c54a4e23841aade1a5c7733192d100184f312c901ec4ebc195edcd37413e99a2cf976c402dc7a66b44fdef0864989d1cd5ed6eeef21d9c91a10032031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950e300a763246964582031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950652474797065687072656f7264657268246f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b69247265766973696f6e016f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c8702470726f746f636f6c56657273696f6e007073616c746564446f6d61696e486173685820aeef16496625994dc47d5b40b464b0984ea56fdac2641c0a6f0823f49731d62111020f88127f2d6afa2a53aecd600b77d64019b408f31001440356c6d6d6bf0c8a02bccaeba586768a207ce81102d8d37cf83a15a8bc4c57b50f81d7a688f44eb6f61001e8e817fd05642a2276c5026aaa22b6d2d829008811029794ecb5493c1b8621e98a555cc4c4352c7e3b1c10013f5377ac0b6160083fd13cef726192cd07bf81f61102a9fb26afa6359abf919a0ebc2441e85cd7835c7810018796a727eb4c3f3239238afae9a8afd87a8cf3d5111102dfab604145ed66cdf0a549339c387139b779dd20100170e4a11d05ca5e05ec6ca0f889df5dd954c6dd461102b28109e1e022060c50018c5de76d49c275a404d3100189842acc9acedbe1bccf85c1ec46d26d5d348ae211"
        )
        val stateTransition =
            HashUtils.fromHex("a6647479706501676f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b697369676e617475726558412090e39108c1ae99e8e054c4e93f86a4af925cb861016eedaae45177ca09fecd1b72c68a2f5ba29697c8ed464ac552db97cb6c37d08091f53d01eeeafaa92126726b7472616e736974696f6e7381a663246964582031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950652474797065687072656f726465726724616374696f6e006824656e74726f70795820853bd951419936a6103630f5dbcb22218124b4e2ceb05607b1ee999aadd15d996f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c87073616c746564446f6d61696e486173685820aeef16496625994dc47d5b40b464b0984ea56fdac2641c0a6f0823f49731d6216f70726f746f636f6c56657273696f6e00747369676e61747572655075626c69634b6579496400")

        val verifyProof =
            DefaultVerifyProof(StateTransitionFactory(StateRepositoryMock()).createFromBuffer(stateTransition) as StateTransitionIdentitySigned).verify(
                Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
            )
        assertTrue(verifyProof)

        println("lib = ${System.getProperty("java.library.path")}")
        val expected_hash = byteArrayOf(217.toByte(), 229.toByte(), 132.toByte(), 217.toByte(), 157.toByte(), 36, 62, 217.toByte(), 111, 90, 191.toByte(), 16, 97, 78, 171.toByte(), 104, 146.toByte(), 207.toByte(), 70, 191.toByte())
        println("expected_hash = $expected_hash")
        val verifyProofMerk = MerkLibVerifyProof(factory.createFromBuffer(stateTransition) as StateTransitionIdentitySigned, expected_hash).verify(
            Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
        )
        assertTrue(verifyProofMerk)

        val verifyProofMerk2 = MerkLibVerifyProof(factory.createFromBuffer(stateTransition) as StateTransitionIdentitySigned, ByteArray(20)).verify(
            Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
        )
        assertFalse(verifyProofMerk2)
    }

    @Test
    fun verifyProofDocumentCreateTest20() {
        MerkVerifyProof.init()
        val proof = HashUtils.fromHex(
            "01c2d5f1c9a54bf3359792e8c528b95c653a4e7e5e02e1026a79d60d0c54a4e23841aade1a5c7733192d100184f312c901ec4ebc195edcd37413e99a2cf976c402dc7a66b44fdef0864989d1cd5ed6eeef21d9c91a10032031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950e300a763246964582031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950652474797065687072656f7264657268246f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b69247265766973696f6e016f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c8702470726f746f636f6c56657273696f6e007073616c746564446f6d61696e486173685820aeef16496625994dc47d5b40b464b0984ea56fdac2641c0a6f0823f49731d62111020f88127f2d6afa2a53aecd600b77d64019b408f31001440356c6d6d6bf0c8a02bccaeba586768a207ce81102d8d37cf83a15a8bc4c57b50f81d7a688f44eb6f61001e8e817fd05642a2276c5026aaa22b6d2d829008811029794ecb5493c1b8621e98a555cc4c4352c7e3b1c10013f5377ac0b6160083fd13cef726192cd07bf81f61102a9fb26afa6359abf919a0ebc2441e85cd7835c7810018796a727eb4c3f3239238afae9a8afd87a8cf3d5111102dfab604145ed66cdf0a549339c387139b779dd20100170e4a11d05ca5e05ec6ca0f889df5dd954c6dd461102b28109e1e022060c50018c5de76d49c275a404d3100189842acc9acedbe1bccf85c1ec46d26d5d348ae211"
        )
        val stateTransition =
            HashUtils.fromHex("a6647479706501676f776e657249645820110c4edc8f64e033d2682c177812ca389725bf0db9cebad89a9f6c00595b684b697369676e617475726558412090e39108c1ae99e8e054c4e93f86a4af925cb861016eedaae45177ca09fecd1b72c68a2f5ba29697c8ed464ac552db97cb6c37d08091f53d01eeeafaa92126726b7472616e736974696f6e7381a663246964582031f95316ab461b89de83da6b517a23b8c42e17b5536dce399b7027c4561df950652474797065687072656f726465726724616374696f6e006824656e74726f70795820853bd951419936a6103630f5dbcb22218124b4e2ceb05607b1ee999aadd15d996f2464617461436f6e747261637449645820ad846420045a41522f868f9c0d5e7e5eafd960e8f34a04588b73583d06a280c87073616c746564446f6d61696e486173685820aeef16496625994dc47d5b40b464b0984ea56fdac2641c0a6f0823f49731d6216f70726f746f636f6c56657273696f6e00747369676e61747572655075626c69634b6579496400")

        val verifyProof =
            DefaultVerifyProof(StateTransitionFactory(StateRepositoryMock()).createFromBuffer(stateTransition) as StateTransitionIdentitySigned).verify(
                Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
            )
        assertTrue(verifyProof)

        println("lib = ${System.getProperty("java.library.path")}")
        val expected_hash = byteArrayOf(217.toByte(), 229.toByte(), 132.toByte(), 217.toByte(), 157.toByte(), 36, 62, 217.toByte(), 111, 90, 191.toByte(), 16, 97, 78, 171.toByte(), 104, 146.toByte(), 207.toByte(), 70, 191.toByte())
        println("expected_hash = $expected_hash")
        val verifyProofMerk = MerkLibVerifyProof(factory.createFromBuffer(stateTransition) as StateTransitionIdentitySigned, expected_hash).verify(
            Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
        )
        assertTrue(verifyProofMerk)

        val verifyProofMerk2 = MerkLibVerifyProof(factory.createFromBuffer(stateTransition) as StateTransitionIdentitySigned, ByteArray(20)).verify(
            Proof(ByteArray(36), proof, byteArrayOf(0), byteArrayOf(0))
        )
        assertFalse(verifyProofMerk2)
    }
}
