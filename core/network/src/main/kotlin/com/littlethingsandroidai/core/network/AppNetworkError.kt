package com.littlethingsandroidai.core.network

sealed interface AppNetworkError {
    data class HttpError(val statusCode: Int, val body: String?) : AppNetworkError

    data class NetworkError(val cause: Throwable) : AppNetworkError

    data class SerializationError(val cause: Throwable) : AppNetworkError

    data class InvalidUrl(val rawUrl: String) : AppNetworkError

    data class UnknownError(val cause: Throwable) : AppNetworkError
}

class AppNetworkException(val error: AppNetworkError) : RuntimeException(error.toString())
