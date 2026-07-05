package com.littlethingsandroidai.service.auth.repository

import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.core.network.AppNetworkError
import com.littlethingsandroidai.core.network.AppNetworkException
import com.littlethingsandroidai.core.network.UniversalResponse
import com.littlethingsandroidai.core.persistence.TokenProvider
import com.littlethingsandroidai.service.auth.request.AuthRequest
import com.littlethingsandroidai.service.dto.LoginInfoDto

interface SessionDataRepositoryType {
    suspend fun refreshToken()
}

class SessionDataRepository(
    private val apiClient: ApiClient,
    private val tokenProvider: TokenProvider,
) : SessionDataRepositoryType {

    override suspend fun refreshToken() {
        val refreshToken =
            tokenProvider.refreshToken
                ?: throw AppNetworkException(
                    AppNetworkError.HttpError(
                        statusCode = 401,
                        body = null,
                    ),
                )

        val request = AuthRequest.RefreshToken(refreshToken = refreshToken)
        val response = apiClient.sendRequest(request)
        val loginInfo: UniversalResponse<LoginInfoDto> = response.parseJson()
        tokenProvider.updateTokens(
            accessToken = loginInfo.data.accessToken,
            refreshToken = loginInfo.data.refreshToken,
        )
    }
}
