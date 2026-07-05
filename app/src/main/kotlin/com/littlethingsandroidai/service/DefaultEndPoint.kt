package com.littlethingsandroidai.service

import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.network.EndPoint

class DefaultEndPoint private constructor(
    private val path: String,
) : EndPoint {

    override fun resolve(environment: AppEnvironment): String {
        val host =
            when (environment) {
                AppEnvironment.DEV -> DEV_AND_STAGING_HOST
                AppEnvironment.STAGING -> DEV_AND_STAGING_HOST
                AppEnvironment.RELEASE -> RELEASE_HOST
            }

        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "https://$host$normalizedPath"
    }

    companion object {
        private const val DEV_AND_STAGING_HOST = "things.dvacode.tech"
        private const val RELEASE_HOST = "api.thelilthings.app"

        fun baseUrl(path: String): EndPoint = DefaultEndPoint(path)
    }
}
