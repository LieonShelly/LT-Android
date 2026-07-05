package com.littlethingsandroidai.core.network

import kotlinx.serialization.Serializable

@Serializable
data class UniversalResponse<T>(
    val code: Int,
    val data: T,
    val message: String? = null,
)
