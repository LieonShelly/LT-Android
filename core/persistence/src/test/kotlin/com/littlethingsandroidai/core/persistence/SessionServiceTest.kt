package com.littlethingsandroidai.core.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionServiceTest {

    private lateinit var storage: FakeRefreshTokenStorage
    private lateinit var sessionService: SessionService

    @Before
    fun setup() {
        storage = FakeRefreshTokenStorage()
        sessionService = SessionService(storage)
    }

    @Test
    fun `updateTokens stores access in memory and refresh in secure storage`() {
        sessionService.updateTokens("access123", "refresh456")

        assertEquals("access123", sessionService.accessToken)
        assertEquals("refresh456", sessionService.refreshToken)
        assertTrue(sessionService.hasValidToken())
    }

    @Test
    fun `clear removes both tokens`() {
        sessionService.updateTokens("access", "refresh")
        sessionService.clear()

        assertNull(sessionService.accessToken)
        assertNull(sessionService.refreshToken)
        assertFalse(sessionService.hasValidToken())
    }

    @Test
    fun `refresh token persists across new SessionService instance`() {
        sessionService.updateTokens("access1", "refresh1")

        val newSession = SessionService(storage)

        assertEquals("refresh1", newSession.refreshToken)
        assertNull(newSession.accessToken)
        assertTrue(newSession.hasValidToken())
    }

    private class FakeRefreshTokenStorage : RefreshTokenStorage {
        private var refreshToken: String? = null

        override fun getRefreshToken(): String? = refreshToken

        override fun saveRefreshToken(token: String) {
            refreshToken = token
        }

        override fun clear() {
            refreshToken = null
        }
    }
}
