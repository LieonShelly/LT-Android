package com.littlethingsandroidai.core.persistence

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

internal interface RefreshTokenStorage {
    fun getRefreshToken(): String?
    fun saveRefreshToken(token: String)
    fun clear()
}

class SecureTokenStorage(context: Context) : RefreshTokenStorage {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun getRefreshToken(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)

    override fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    override fun clear() {
        sharedPreferences.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "session_tokens"
        private const val KEY_REFRESH_TOKEN = "refreshToken"
    }
}
