package com.littlethingsandroidai.domain.signin

object SignInDevConfig {
    /** Set to false to restore real Google Sign-In + backend auth. */
    const val MOCK_GOOGLE_SIGN_IN = true

    const val MOCK_ACCESS_TOKEN = "dev-mock-access-token"
    const val MOCK_REFRESH_TOKEN = "dev-mock-refresh-token"
}
