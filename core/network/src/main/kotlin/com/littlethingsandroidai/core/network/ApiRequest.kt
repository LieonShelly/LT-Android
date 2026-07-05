package com.littlethingsandroidai.core.network

interface ApiRequest {
    val endPoint: EndPoint
    val method: HttpMethod
    val payload: HttpPayload
}
