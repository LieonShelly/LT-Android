package com.littlethingsandroidai.service.interceptor

import com.littlethingsandroidai.core.persistence.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

class LogoutInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (shouldTriggerLogout(response)) {
            tokenProvider.clear()
            SessionEvents.publish(SessionEvent.SessionExpired)
        }
        return response
    }

    private fun shouldTriggerLogout(response: Response): Boolean {
        val refreshFailed = response.header(RefreshTokenInterceptor.REFRESH_FAILED_HEADER) == TRUE_VALUE
        return refreshFailed || response.code == HTTP_UNAUTHORIZED
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val TRUE_VALUE = "true"
    }
}
