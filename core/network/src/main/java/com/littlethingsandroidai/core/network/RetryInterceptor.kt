package com.littlethingsandroidai.core.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor(private val maxRetry: Int = 2) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastIOException: IOException? = null
        var response: Response? = null

        while (attempt <= maxRetry) {
            try {
                response = chain.proceed(chain.request())
                if (!response.isSuccessful && response.code in 500..599 && attempt < maxRetry) {
                    response.close()
                    attempt++
                    continue
                }
                return response
            } catch (ioe: IOException) {
                lastIOException = ioe
                if (attempt >= maxRetry) throw ioe
                attempt++
            }
        }

        response?.close()
        throw lastIOException ?: IOException("Request failed after retries")
    }
}
