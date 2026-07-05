package com.littlethingsandroidai.service.auth

import com.littlethingsandroidai.service.auth.repository.AuthRepository
import com.littlethingsandroidai.service.auth.usecase.AuthUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCaseTest {

    @Test
    fun executeGoogleLogin_callsRepositoryWithIdToken() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = AuthUseCase(repository = repository)

        useCase.executeGoogleLogin(idToken = "google-id-token")

        assertEquals("google-id-token", repository.lastGoogleLoginToken)
    }

    @Test
    fun logout_callsRepositoryLogout() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = AuthUseCase(repository = repository)

        useCase.logout()

        assertTrue(repository.logoutCalled)
    }
}

private class FakeAuthRepository : AuthRepository {
    var lastGoogleLoginToken: String? = null
    var logoutCalled: Boolean = false

    override suspend fun googleLogin(idToken: String) {
        lastGoogleLoginToken = idToken
    }

    override suspend fun logout() {
        logoutCalled = true
    }
}
