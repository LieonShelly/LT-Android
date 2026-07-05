package com.littlethingsandroidai.core.persistence

import android.content.Context

class SessionService internal constructor(
    private val storage: RefreshTokenStorage,
) : TokenProvider {

    constructor(context: Context) : this(SecureTokenStorage(context))

    @Volatile
    private var _accessToken: String? = null

    override val accessToken: String?
        get() = _accessToken

    override val refreshToken: String?
        get() = storage.getRefreshToken()

    override fun updateTokens(accessToken: String, refreshToken: String) {
        _accessToken = accessToken
        storage.saveRefreshToken(refreshToken)
    }

    override fun clear() {
        _accessToken = null
        storage.clear()
    }

    override fun hasValidToken(): Boolean = !refreshToken.isNullOrBlank()
}
