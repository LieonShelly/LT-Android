package com.littlethingsandroidai.service.interceptor

import com.littlethingsandroidai.core.persistence.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.accessToken
        if (token.isNullOrBlank()) {
            return chain.proceed(chain.request())
        }

        val authenticatedRequest =
            chain.request()
                .newBuilder()
                .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX $token")
                .build()
        return chain.proceed(authenticatedRequest)
    }

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer"
    }
}
