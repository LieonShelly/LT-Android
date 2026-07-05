package com.littlethingsandroidai.core.network

import com.littlethingsandroidai.core.common.AppEnvironment
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ApiClientTest {
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        TestFixtures.baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `sendRequest returns body on 200`() = runTest {
        val expectedBody = """{"code":0,"data":{"hello":"world"},"message":"ok"}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(expectedBody))

        val apiClient = ApiClient(environment = AppEnvironment.DEV)
        val response = apiClient.sendRequest(TestRequest.FetchHello)

        assertEquals(200, response.code)
        assertEquals(expectedBody, response.body)
        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `sendRequest retries on 503 up to maxRetry plus one attempts`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("unavailable-1"))
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("unavailable-2"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":0,"data":"ok"}"""))

        val apiClient = ApiClient(environment = AppEnvironment.DEV, maxRetry = 2)
        val response = apiClient.sendRequest(TestRequest.FetchHello)

        assertEquals(200, response.code)
        assertEquals(3, mockWebServer.requestCount)
    }
}

private object TestFixtures {
    var baseUrl: String = ""
}

private sealed class TestRequest(
    override val endPoint: EndPoint,
    override val method: HttpMethod,
    override val payload: HttpPayload,
) : ApiRequest {
    data object FetchHello : TestRequest(
        endPoint = TestEndPoint("/v1/hello"),
        method = HttpMethod.GET,
        payload = HttpPayload.Empty,
    )
}

private class TestEndPoint(private val path: String) : EndPoint {
    override fun resolve(environment: AppEnvironment): String = "${TestFixtures.baseUrl}$path"
}
