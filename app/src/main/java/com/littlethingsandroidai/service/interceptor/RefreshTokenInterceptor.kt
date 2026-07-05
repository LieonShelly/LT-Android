package com.littlethingsandroidai.service.interceptor

import com.littlethingsandroidai.core.persistence.TokenProvider
import com.littlethingsandroidai.service.AppDataWithoutAuthorizationServiceful
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response

class RefreshTokenInterceptor(
    private val tokenProvider: TokenProvider,
    private val appDataWithoutAuthorizationService: AppDataWithoutAuthorizationServiceful,
) : Interceptor {
    private val refreshMutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val isRetriedRequest = request.header(RETRY_HEADER) == RETRY_HEADER_VALUE
        if (response.code != HTTP_UNAUTHORIZED || isRetriedRequest) {
            return response
        }

        val accessTokenBeforeRefresh = tokenProvider.accessToken
        val refreshSucceeded =
            runBlocking {
                refreshMutex.withLock {
                    val latestAccessToken = tokenProvider.accessToken
                    val alreadyRefreshedByAnotherRequest =
                        !latestAccessToken.isNullOrBlank() && latestAccessToken != accessTokenBeforeRefresh

                    if (alreadyRefreshedByAnotherRequest) {
                        true
                    } else {
                        runCatching {
                            appDataWithoutAuthorizationService.refreshTokenUseCase.execute()
                        }.isSuccess
                    }
                }
            }

        val refreshedAccessToken = tokenProvider.accessToken
        if (!refreshSucceeded || refreshedAccessToken.isNullOrBlank()) {
            return response.newBuilder()
                .header(REFRESH_FAILED_HEADER, TRUE_VALUE)
                .build()
        }

        response.close()
        val retriedRequest =
            request.newBuilder()
                .header(RETRY_HEADER, RETRY_HEADER_VALUE)
                .header(AuthInterceptor.AUTHORIZATION_HEADER, "${AuthInterceptor.BEARER_PREFIX} $refreshedAccessToken")
                .build()
        return chain.proceed(retriedRequest)
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val RETRY_HEADER = "X-LT-Retry"
        private const val RETRY_HEADER_VALUE = "true"
        const val REFRESH_FAILED_HEADER = "X-LT-Refresh-Failed"
        private const val TRUE_VALUE = "true"
    }
}
