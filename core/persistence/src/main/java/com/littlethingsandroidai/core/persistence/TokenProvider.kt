package com.littlethingsandroidai.core.persistence

interface TokenProvider {
    val accessToken: String?
    val refreshToken: String?
    fun updateTokens(accessToken: String, refreshToken: String)
    fun clear()
    fun hasValidToken(): Boolean = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()
}
