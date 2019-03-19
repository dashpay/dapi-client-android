package org.dashevo.dapiclient

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.only
import com.nhaarman.mockitokotlin2.verify
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.dashevo.dapiclient.callback.DapiRequestCallback
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.schema.Schema
import org.dashevo.schema.SchemaLoader
import org.jsonorg.JSONObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File


@DisplayName("Dapi Client Tests")
class DapiClientTest : BaseTest() {

    val sleepTime = 100L
    val username = "alice"

    companion object {
        val mockWebServer = MockWebServer()
        init {
            mockWebServer.start(8080)

            Schema.schemaLoader = object : SchemaLoader {
                override fun loadJsonSchema(): JSONObject {
                    return JSONObject(File(Schema::class.java.getResource("/schema_v7.json").path).readText())
                }

                override fun loadDashSystemSchema(): JSONObject {
                    return JSONObject(File(Schema::class.java.getResource("/dash_system_schema.json").path).readText())
                }
            }
        }
    }

    fun createDapiClient(): DapiClient {
        return DapiClient("http://127.0.0.1", "8080")
    }

    fun enqueueRestCall(jsonName: String, responseCode: Int) {
        val mockedResponse = MockResponse().setResponseCode(responseCode)
                .setBody(getJson(jsonName))
        mockWebServer.enqueue(mockedResponse)
    }

    @Nested
    @DisplayName("User Tests")
    inner class UserTests {

        @Test
        @DisplayName("Get User by username")
        fun getByUsername() {
            enqueueRestCall("get_user_response", 200)
            val cbMock = mock<DapiRequestCallback<BlockchainUser>>()
            createDapiClient().getUser(username, cbMock)
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onSuccess(any())
        }

        @Test
        @DisplayName("Get non-existent User by username")
        fun getByUsernameError() {
            enqueueRestCall("get_user_response_error", 404)
            val cbMock = mock<DapiRequestCallback<BlockchainUser>>()
            createDapiClient().getUser(username, cbMock)
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onError(any())
        }

    }

    @Nested
    @DisplayName("Dap Tests")
    inner class DapTests {

        @Test
        @DisplayName("Fetch Dap")
        fun fetchDapContract() {
            enqueueRestCall("fetch_dap_contract_response", 200)
            val cbMock = mock<DapiRequestCallback<HashMap<String, String>>>()
            createDapiClient().fetchDapContract("6c9f91a044e7db796e48ae3a8e1392f6d62cca3d70b42406cc46781563fb43dc", cbMock)
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onSuccess(any())
        }

        @Test
        @DisplayName("Fetch Dap Contract Error")
        fun fetchDapContractError() {
            enqueueRestCall("fetch_dap_contract_error", 400)
            val cbMock = mock<DapiRequestCallback<HashMap<String, String>>>()
            createDapiClient().fetchDapContract("6c9f91a044e7db796e48ae3a8e1392f6d62cca3d70b42406cc46781563fb43dc", cbMock)
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Fetch Dap Objects")
        fun fetchDapObjects() {
            enqueueRestCall("fetch_dap_objects_response", 200)
            val cbMock = mock<DapiRequestCallback<List<Any>>>()
            createDapiClient().fetchDapObjects("", "", cbMock, hashMapOf())
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onSuccess(any())
        }

        @Test
        @DisplayName("Fetch Dap Objects Error")
        fun fetchDapObjectsError() {
            enqueueRestCall("fetch_dap_objects_error", 400)
            val cbMock = mock<DapiRequestCallback<List<Any>>>()
            createDapiClient().fetchDapObjects("", "", cbMock, hashMapOf())
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Send Dap Object")
        fun sendDapObject() {
            enqueueRestCall("send_dap_object_response", 200)
            val cbMock = mock<DapiRequestCallback<String>>()
            val emptySha256Hash = Sha256Hash.ZERO_HASH
            val emptyPrivateKey = ECKey()
            createDapiClient().sendDapObject(JSONObject(), "", emptySha256Hash, emptySha256Hash,
                    emptyPrivateKey, cbMock)
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onSuccess(any())
        }

        @Test
        @DisplayName("Send Dap Object Error")
        fun sendDapObjectError() {
            enqueueRestCall("send_dap_object_error", 400)
            val cbMock = mock<DapiRequestCallback<String>>()
            val emptySha256Hash = Sha256Hash.ZERO_HASH
            val emptyPrivateKey = ECKey()
            createDapiClient().sendDapObject(JSONObject(), "", emptySha256Hash, emptySha256Hash,
                    emptyPrivateKey, cbMock)
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Register Dap Contract")
        fun registerDapContract() {
            enqueueRestCall("send_dap_object_response", 200)
            val cbMock = mock<DapiRequestCallback<String>>()
            val emptySha256Hash = Sha256Hash.ZERO_HASH
            val emptyPrivateKey = ECKey()
            val dapContract = JSONObject(File(Schema::class.java.getResource("/dashpay_schema.json").path).readText())
            createDapiClient().registerDap(dapContract, emptySha256Hash, emptySha256Hash,
                    emptyPrivateKey, cbMock)
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onSuccess(any())
        }

        @Test
        @DisplayName("Register Invalid Dap Contract")
        fun registerInvalidDapContract() {
            val cbMock = mock<DapiRequestCallback<String>>()
            val emptySha256Hash = Sha256Hash.ZERO_HASH
            val emptyPrivateKey = ECKey()
            val dapContract = JSONObject(File(Schema::class.java.getResource("/invalid_schema.json").path).readText())
            createDapiClient().registerDap(dapContract, emptySha256Hash, emptySha256Hash,
                    emptyPrivateKey, cbMock)
            Thread.sleep(sleepTime)
            verify(cbMock, only()).onError(any())
        }

    }
}