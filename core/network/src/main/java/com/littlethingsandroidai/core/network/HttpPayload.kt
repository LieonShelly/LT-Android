package com.littlethingsandroidai.core.network

sealed interface HttpPayload {
    data object Empty : HttpPayload

    data class Json(val body: Map<String, Any?>) : HttpPayload

    data class UrlEncoding(val params: List<Pair<String, String>>) : HttpPayload
}
