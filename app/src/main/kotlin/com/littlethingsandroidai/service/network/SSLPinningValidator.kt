package com.littlethingsandroidai.service.network

import com.littlethingsandroidai.core.common.AppEnvironment
import okhttp3.OkHttpClient

object SSLPinningValidator {
    fun createBuilder(environment: AppEnvironment): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
        if (environment != AppEnvironment.DEV) {
            // TODO: Add certificate pinner for STAGING/RELEASE.
        }
        return builder
    }
}
