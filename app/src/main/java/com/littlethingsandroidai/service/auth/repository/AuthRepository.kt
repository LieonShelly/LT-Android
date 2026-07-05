package com.littlethingsandroidai.service.auth.repository

import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.core.network.UniversalResponse
import com.littlethingsandroidai.core.persistence.TokenProvider
import com.littlethingsandroidai.service.auth.request.AuthRequest
import com.littlethingsandroidai.service.dto.LoginInfoDto

interface AuthRepository {
    suspend fun googleLogin(idToken: String)
    suspend fun logout()
}

class DefaultAuthRepository(
    private val apiClient: ApiClient,
    private val tokenProvider: TokenProvider,
) : AuthRepository {
    override suspend fun googleLogin(idToken: String) {
        val request = AuthRequest.GoogleLogin(idToken = idToken)
        val response = apiClient.sendRequest(request)
        val loginInfo: UniversalResponse<LoginInfoDto> = response.parseJson()
        tokenProvider.updateTokens(
            accessToken = loginInfo.data.accessToken,
            refreshToken = loginInfo.data.refreshToken,
        )
    }

    override suspend fun logout() {
        tokenProvider.clear()
    }
}
