package com.littlethingsandroidai.core.network

import kotlinx.serialization.Serializable

@Serializable
data class UniversalResponse<T>(
    val success: Boolean = true,
    val code: Int = 0,
    val data: T,
    val message: String? = null,
)
