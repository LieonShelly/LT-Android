package com.littlethingsandroidai.service.interceptor

import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.core.persistence.TokenProvider
import com.littlethingsandroidai.service.AppDataWithoutAuthorizationService
import com.littlethingsandroidai.service.auth.repository.SessionDataRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RefreshTokenInterceptorTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenProvider: FakeTokenProvider

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        tokenProvider = FakeTokenProvider(accessToken = "old-access", refreshToken = "stored-refresh")
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `401 triggers refresh and retries request with new access token`() {
        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        val client = buildAuthenticatedClient(baseUrl)

        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"code":0,"data":{"access_token":"new-access","refresh_token":"new-refresh"},"message":"ok"}""",
                ),
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val response =
            client
                .newCall(
                    Request.Builder()
                        .url("$baseUrl/api/me")
                        .build(),
                ).execute()

        assertTrue(response.isSuccessful)
        assertEquals(200, response.code)
        assertEquals("new-access", tokenProvider.accessToken)
        assertEquals("new-refresh", tokenProvider.refreshToken)
        assertEquals(3, mockWebServer.requestCount)

        val initialRequest = mockWebServer.takeRequest()
        val refreshRequest = mockWebServer.takeRequest()
        val retriedRequest = mockWebServer.takeRequest()

        assertEquals("/api/me", initialRequest.path)
        assertEquals("/api/auth/refresh", refreshRequest.path)
        assertEquals("/api/me", retriedRequest.path)
        assertEquals("Bearer new-access", retriedRequest.getHeader(AuthInterceptor.AUTHORIZATION_HEADER))
        assertEquals("true", retriedRequest.getHeader("X-LT-Retry"))
    }

    @Test
    fun `refresh failure clears session via logout interceptor`() {
        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        val client = buildAuthenticatedClient(baseUrl)

        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":401,"message":"expired"}"""))

        val response =
            client
                .newCall(
                    Request.Builder()
                        .url("$baseUrl/api/me")
                        .build(),
                ).execute()

        assertEquals(401, response.code)
        assertFalse(tokenProvider.hasValidToken())
        assertEquals(
            "true",
            response.header(RefreshTokenInterceptor.REFRESH_FAILED_HEADER),
        )
    }

    /*
     * Manual verification (dev build):
     * 1. Sign in with a valid Google account.
     * 2. Force the backend/dev proxy to return 401 on an authenticated call.
     * 3. Confirm the request is retried once and the user stays signed in.
     * 4. Invalidate the refresh token and repeat; app should return to SignIn via Splash.
     */
    private fun buildAuthenticatedClient(baseUrl: String): OkHttpClient {
        val bareApiClient =
            ApiClient(
                environment = AppEnvironment.DEV,
                baseUrlOverride = baseUrl,
            )
        val appDataWithoutAuthorizationService =
            AppDataWithoutAuthorizationService(
                sessionDataRepository =
                    SessionDataRepository(
                        apiClient = bareApiClient,
                        tokenProvider = tokenProvider,
                    ),
            )

        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(LogoutInterceptor(tokenProvider))
            .addInterceptor(
                RefreshTokenInterceptor(
                    tokenProvider = tokenProvider,
                    appDataWithoutAuthorizationService = appDataWithoutAuthorizationService,
                ),
            )
            .build()
    }

    private class FakeTokenProvider(
        accessToken: String?,
        refreshToken: String?,
    ) : TokenProvider {
        private var storedAccessToken: String? = accessToken
        private var storedRefreshToken: String? = refreshToken

        override val accessToken: String?
            get() = storedAccessToken

        override val refreshToken: String?
            get() = storedRefreshToken

        override fun updateTokens(accessToken: String, refreshToken: String) {
            storedAccessToken = accessToken
            storedRefreshToken = refreshToken
        }

        override fun clear() {
            storedAccessToken = null
            storedRefreshToken = null
        }
    }
}
