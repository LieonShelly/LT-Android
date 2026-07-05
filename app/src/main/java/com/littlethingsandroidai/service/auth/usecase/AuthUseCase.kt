package com.littlethingsandroidai.service.auth.usecase

import com.littlethingsandroidai.service.auth.repository.AuthRepository

interface AuthUseCaseType {
    suspend fun executeGoogleLogin(idToken: String)
    suspend fun logout()
}

class AuthUseCase(
    private val repository: AuthRepository,
) : AuthUseCaseType {
    override suspend fun executeGoogleLogin(idToken: String) {
        repository.googleLogin(idToken = idToken)
    }

    override suspend fun logout() {
        repository.logout()
    }
}
