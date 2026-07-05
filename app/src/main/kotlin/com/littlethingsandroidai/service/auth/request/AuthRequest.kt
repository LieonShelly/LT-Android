package com.littlethingsandroidai.service.auth.request

import com.littlethingsandroidai.core.network.ApiRequest
import com.littlethingsandroidai.core.network.EndPoint
import com.littlethingsandroidai.core.network.HttpMethod
import com.littlethingsandroidai.core.network.HttpPayload
import com.littlethingsandroidai.service.DefaultEndPoint

sealed class AuthRequest : ApiRequest {

    data class GoogleLogin(private val idToken: String) : AuthRequest() {
        override val endPoint: EndPoint = DefaultEndPoint.baseUrl(path = "/api/auth/google")
        override val method: HttpMethod = HttpMethod.POST
        override val payload: HttpPayload = HttpPayload.Json(body = mapOf("idToken" to idToken))
    }

    data class RefreshToken(private val refreshToken: String) : AuthRequest() {
        override val endPoint: EndPoint = DefaultEndPoint.baseUrl(path = "/api/auth/refresh")
        override val method: HttpMethod = HttpMethod.POST
        override val payload: HttpPayload = HttpPayload.Json(body = mapOf("refresh_token" to refreshToken))
    }
}
