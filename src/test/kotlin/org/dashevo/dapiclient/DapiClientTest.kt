package org.dashevo.dapiclient

import com.nhaarman.mockitokotlin2.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.dashevo.dapiclient.callback.*
import org.dashevo.dapiclient.model.BlockchainUserContainer
import org.dashevo.dapiclient.model.DapContext
import org.dashevo.dapiclient.model.DapSpace
import org.json.JSONObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


@DisplayName("Dapi Client Tests")
class DapiClientTest : BaseTest() {

    val sleepTime = 500L
    val username = "alice"
    val userId = "87153bca4e5a0c449061dc54696cb951488575b4d8964f0e54243c8336fc39fb"
    val pubKey = "024964f06ea5cfec1890d7e526653b083c12360f79164c1e8163327d0849fa7bca"

    companion object {
        val mockWebServer = MockWebServer()
        init {
            mockWebServer.start(8080)
        }
    }

    fun createDapiClient(): DapiClient {
        return DapiClient("localhost", "8080")
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
        @DisplayName("Create a Blockchain User")
        fun createUser() {
            enqueueRestCall("create_user_response", 200)

            val cbMock = mock<CreateUserCallback>()
            createDapiClient().createUser(username, pubKey, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any(), any())
        }

        @Test
        @DisplayName("Create a duplicated Blockchain User")
        fun createUserError() {
            enqueueRestCall("create_user_error", 400)

            val cbMock = mock<CreateUserCallback>()
            createDapiClient().createUser(username, pubKey, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Search for existing user")
        fun searchUser() {
            enqueueRestCall("search_users_response", 200)

            val cbMock = mock<SearchUsersCallback>()
            createDapiClient().searchUsers("ali", cbMock)

            Thread.sleep(sleepTime)

            val argumentCaptor = argumentCaptor<List<BlockchainUserContainer>>()
            verify(cbMock, only()).onSuccess(argumentCaptor.capture())

            assert(argumentCaptor.firstValue.isNotEmpty())
        }

        @Test
        @DisplayName("Search for non-existent user")
        fun searchUserEmpty() {
            enqueueRestCall("search_users_empty_response", 200)

            val cbMock = mock<SearchUsersCallback>()
            createDapiClient().searchUsers("bob", cbMock)

            Thread.sleep(sleepTime)

            val argumentCaptor = argumentCaptor<List<BlockchainUserContainer>>()
            verify(cbMock, only()).onSuccess(argumentCaptor.capture())

            assert(argumentCaptor.firstValue.isEmpty())
        }

        @Test
        @DisplayName("Search user error")
        fun searchUserError() {
            enqueueRestCall("get_user_response_error", 400)

            val cbMock = mock<SearchUsersCallback>()
            createDapiClient().searchUsers("bob", cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Get User by username")
        fun getByUsername() {
            enqueueRestCall("get_user_response", 200)

            val cbMock = mock<GetUserCallback>()
            createDapiClient().getUser(username, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any())
        }

        @Test
        @DisplayName("Get non-existent User by username")
        fun getByUsernameError() {
            enqueueRestCall("get_user_response_error", 404)

            val cbMock = mock<GetUserCallback>()
            createDapiClient().getUser(username, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Login")
        fun login() {
            val dapiClient = createDapiClient()
            enqueueRestCall("get_user_response", 200)

            val cbMock = mock<LoginCallback>()
            dapiClient.login(username, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any())
            assert(dapiClient.checkAuth())
            assert(!dapiClient.checkDap())
        }

        @Test
        @DisplayName("Login non-existent User")
        fun loginNonExistent() {
            val dapiClient = createDapiClient()
            enqueueRestCall("get_user_response_error", 400)

            val cbMock = mock<LoginCallback>()
            dapiClient.login(username, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
            assert(!dapiClient.checkAuth())
            assert(!dapiClient.checkDap())
        }

        @Test
        @DisplayName("Login invalid blockchain User")
        fun loginInvalidBlockchainUser() {
            val dapiClient = createDapiClient()
            enqueueRestCall("get_user_response_invalid_blockchain_user", 200)

            val cbMock = mock<LoginCallback>()
            dapiClient.login(username, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
            assert(!dapiClient.checkAuth())
            assert(!dapiClient.checkDap())
        }

        @Test
        @DisplayName("Login and fetch dap context")
        fun loginAndFetchDapContext() {
            val dapiClient = createDapiClient()
            enqueueRestCall("get_dap_response", 200)

            val getDapCbMock = mock<DapCallback>()
            dapiClient.getDap("7c9f91a044e7db796e48ae3a8e1392f6d62cca3d70b42406cc46781563fb43dc", getDapCbMock)

            Thread.sleep(sleepTime)

            enqueueRestCall("get_user_response", 200)
            enqueueRestCall("get_dapcontext_response", 200)

            val cbMock = mock<LoginCallback>()
            dapiClient.login(username, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any())
            assert(dapiClient.checkAuth())
            assert(dapiClient.checkDap())
        }

        @Test
        @DisplayName("Login and fail to fetch dap")
        fun loginAndFailFetchDap() {
            val dapiClient = createDapiClient()
            enqueueRestCall("get_dap_error", 400)

            val getDapCbMock = mock<DapCallback>()
            dapiClient.getDap("7c9f91a044e7db796e48ae3a8e1392f6d62cca3d70b42406cc46781563fb43dc", getDapCbMock)

            Thread.sleep(sleepTime)

            enqueueRestCall("get_user_response", 200)

            val cbMock = mock<LoginCallback>()
            dapiClient.login(username, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any())
            assert(dapiClient.checkAuth())
            assert(!dapiClient.checkDap())
        }

        @Test
        @DisplayName("Login and fail fetch dap context")
        fun loginAndFailFetchDapContext() {
            val dapiClient = createDapiClient()
            enqueueRestCall("get_dap_response", 200)

            val getDapCbMock = mock<DapCallback>()
            dapiClient.getDap("7c9f91a044e7db796e48ae3a8e1392f6d62cca3d70b42406cc46781563fb43dc", getDapCbMock)

            Thread.sleep(sleepTime)

            enqueueRestCall("get_user_response", 200)
            enqueueRestCall("get_dapcontext_error", 400)

            val cbMock = mock<LoginCallback>()
            dapiClient.login(username, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any())
            assert(dapiClient.checkAuth())
            assert(dapiClient.checkDap())
        }

        @Test
        @DisplayName("Logout")
        fun logout() {
            val dapiClient = createDapiClient()

            dapiClient.logout()

            assert(!dapiClient.checkAuth())
            assert(!dapiClient.checkDap())
        }
    }

    @Nested
    @DisplayName("Dap Tests")
    inner class DapTests {

        private fun login(dapiClient: DapiClient) {
            enqueueRestCall("get_user_response", 200)
            dapiClient.login(username, mock())
            Thread.sleep(sleepTime)
        }

        private fun getDap(dapiClient: DapiClient) {
            enqueueRestCall("get_dap_response", 200)
            dapiClient.getDap("7c9f91a044e7db796e48ae3a8e1392f6d62cca3d70b42406cc46781563fb43dc", mock())
            Thread.sleep(sleepTime)
        }

        @Test
        @DisplayName("Create Dap")
        fun createDap() {
            val dapiClient = createDapiClient()
            val dapJson = JSONObject(getJson("dashpay-dap"))
            enqueueRestCall("create_dap_response", 200)

            val cbMock = mock<DapCallback>()
            dapiClient.createDap(dapJson, userId, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any())
            assert(dapiClient.checkDap())
        }

        @Test
        @DisplayName("Create Dap error")
        fun createDapError() {
            val dapiClient = createDapiClient()
            val dapJson = JSONObject(getJson("dashpay-dap"))
            enqueueRestCall("create_dap_error", 400)

            val cbMock = mock<DapCallback>()
            dapiClient.createDap(dapJson, userId, cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
            assert(!dapiClient.checkDap())
        }

        @Test
        @DisplayName("Get Dap")
        fun getDap() {
            val dapiClient = createDapiClient()
            enqueueRestCall("get_dap_response", 200)

            val cbMock = mock<DapCallback>()
            dapiClient.getDap("7c9f91a044e7db796e48ae3a8e1392f6d62cca3d70b42406cc46781563fb43dc", cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any())
            assert(dapiClient.checkDap())
        }

        @Test
        @DisplayName("Get Dap error")
        fun getDapError() {
            val dapiClient = createDapiClient()
            enqueueRestCall("get_dap_error", 404)

            val cbMock = mock<DapCallback>()
            dapiClient.getDap("7c9f91a044e7db796e48ae3a8e1392f6d62cca3d70b42406cc46781563fb43dc", cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
            assert(!dapiClient.checkDap())
        }

        @Test
        @DisplayName("Send Dap Object")
        fun sendDapObject() {
            val dapiClient = createDapiClient()

            //Login
            login(dapiClient)

            //Get Dap
            getDap(dapiClient)

            //Send Dap Object
            enqueueRestCall("send_dapobject_response", 200)
            val cbMock = mock<CommitDapObjectCallback>()
            dapiClient.commitSingleObject(JSONObject(), cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any(), any())
            assert(dapiClient.checkDap())
        }

        @Test
        @DisplayName("Send Dap Object error")
        fun sendDapObjectError() {
            val dapiClient = createDapiClient()

            login(dapiClient)
            getDap(dapiClient)

            //Send Dap Object
            enqueueRestCall("send_dapobject_error", 400)
            val cbMock = mock<CommitDapObjectCallback>()
            dapiClient.commitSingleObject(JSONObject(), cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
            assert(dapiClient.checkDap())
        }

        @Test
        @DisplayName("Send Dap Object without user")
        fun sendDapObjectWithoutUser() {
            val dapiClient = createDapiClient()

            getDap(dapiClient)

            //Send Dap Object
            val cbMock = mock<CommitDapObjectCallback>()
            dapiClient.commitSingleObject(JSONObject(), cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
            assert(dapiClient.checkDap())
            assert(!dapiClient.checkAuth())
        }

        @Test
        @DisplayName("Send Dap Object without Dap")
        fun sendDapObjectWithoutDap() {
            val dapiClient = createDapiClient()

            login(dapiClient)

            //Send Dap Object
            val cbMock = mock<CommitDapObjectCallback>()
            dapiClient.commitSingleObject(JSONObject(), cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
            assert(!dapiClient.checkDap())
            assert(dapiClient.checkAuth())
        }

        @Test
        @DisplayName("Get Dap Space")
        fun getDapSpace() {
            val dapiClient = createDapiClient()

            login(dapiClient)
            getDap(dapiClient)

            enqueueRestCall("get_dapspace_response", 200)
            val cbMock = mock<GetDapSpaceCallback<DapSpace>>()
            dapiClient.getDapSpace(cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onSuccess(any())
        }

        @Test
        @DisplayName("Get Dap Space without User")
        fun getDapSpaceWithoutUser() {
            val dapiClient = createDapiClient()

            getDap(dapiClient)

            val cbMock = mock<GetDapSpaceCallback<DapSpace>>()
            dapiClient.getDapSpace(cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }


        @Test
        @DisplayName("Get Dap Space without Dap")
        fun getDapSpaceWithoutDap() {
            val dapiClient = createDapiClient()

            login(dapiClient)

            val cbMock = mock<GetDapSpaceCallback<DapSpace>>()
            dapiClient.getDapSpace(cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Get Dap Space error")
        fun getDapSpaceError() {
            val dapiClient = createDapiClient()

            login(dapiClient)
            getDap(dapiClient)

            enqueueRestCall("get_dapspace_error", 400)
            val cbMock = mock<GetDapSpaceCallback<DapSpace>>()
            dapiClient.getDapSpace(cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Get Dap Context")
        fun getDapContext() {
            val dapiClient = createDapiClient()

            login(dapiClient)
            getDap(dapiClient)

            enqueueRestCall("get_dapcontext_response", 200)
            val cbMock = mock<GetDapSpaceCallback<DapContext>>()
            dapiClient.getDapContext(cbMock)

            Thread.sleep(sleepTime)

            val dapContextCaptor = argumentCaptor<DapContext>()

            verify(cbMock, only()).onSuccess(dapContextCaptor.capture())
            assert(dapContextCaptor.firstValue.related?.isNotEmpty() ?: any())
        }

        @Test
        @DisplayName("Get Dap Context without user")
        fun getDapContextWithoutUser() {
            val dapiClient = createDapiClient()

            getDap(dapiClient)

            val cbMock = mock<GetDapSpaceCallback<DapContext>>()
            dapiClient.getDapContext(cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }


        @Test
        @DisplayName("Get Dap Context without Dap")
        fun getDapContextWithoutDap() {
            val dapiClient = createDapiClient()

            login(dapiClient)

            val cbMock = mock<GetDapSpaceCallback<DapContext>>()
            dapiClient.getDapContext(cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }

        @Test
        @DisplayName("Get Dap Context with no relations")
        fun getDapContextNoRelations() {
            val dapiClient = createDapiClient()

            login(dapiClient)
            getDap(dapiClient)

            enqueueRestCall("get_dapcontext_no_relations_response", 200)
            val cbMock = mock<GetDapSpaceCallback<DapContext>>()
            dapiClient.getDapContext(cbMock)

            Thread.sleep(sleepTime)

            val dapContextCaptor = argumentCaptor<DapContext>()

            verify(cbMock, only()).onSuccess(dapContextCaptor.capture())
            assert(dapContextCaptor.firstValue.related == null)
        }

        @Test
        @DisplayName("Get Dap Context error")
        fun getDapContextError() {
            val dapiClient = createDapiClient()

            login(dapiClient)
            getDap(dapiClient)

            enqueueRestCall("get_dapcontext_error", 400)
            val cbMock = mock<GetDapSpaceCallback<DapContext>>()
            dapiClient.getDapContext(cbMock)

            Thread.sleep(sleepTime)

            verify(cbMock, only()).onError(any())
        }

    }

}