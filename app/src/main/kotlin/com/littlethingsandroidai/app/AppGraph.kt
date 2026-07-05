package com.littlethingsandroidai.app

import android.content.Context
import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.common.feature.FeatureRolloutStage
import com.littlethingsandroidai.core.common.feature.FeatureToggle
import com.littlethingsandroidai.core.common.injection.InjectionValues
import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.core.persistence.SessionService
import com.littlethingsandroidai.service.AppDataWithAuthorizationService
import com.littlethingsandroidai.service.AppDataWithoutAuthorizationService
import com.littlethingsandroidai.service.auth.repository.AuthRepository
import com.littlethingsandroidai.service.auth.repository.DefaultAuthRepository
import com.littlethingsandroidai.service.auth.repository.SessionDataRepository
import com.littlethingsandroidai.service.interceptor.AuthInterceptor
import com.littlethingsandroidai.service.interceptor.LogoutInterceptor
import com.littlethingsandroidai.service.interceptor.RefreshTokenInterceptor
import com.littlethingsandroidai.service.network.SSLPinningValidator

class AppGraph private constructor(
    val environment: AppEnvironment,
    val sessionService: SessionService,
    val appDataWithoutAuthorizationService: AppDataWithoutAuthorizationService,
    val appDataWithAuthorizationService: AppDataWithAuthorizationService,
    val bareApiClient: ApiClient,
    val authenticatedApiClient: ApiClient,
    val authRepository: AuthRepository,
) {
    companion object {
        lateinit var current: AppGraph
            private set

        @Synchronized
        fun build(context: Context, environment: AppEnvironment) {
            val appContext = context.applicationContext
            val sessionService = SessionService(appContext)

            // Placeholder hook for future SSL pinning setup.
            SSLPinningValidator.createBuilder(environment)

            val bareApiClient = ApiClient(environment = environment)
            val sessionDataRepository =
                SessionDataRepository(
                    apiClient = bareApiClient,
                    tokenProvider = sessionService,
                )
            val appDataWithoutAuthorizationService =
                AppDataWithoutAuthorizationService(sessionDataRepository)

            val authInterceptor = AuthInterceptor(tokenProvider = sessionService)
            val refreshTokenInterceptor =
                RefreshTokenInterceptor(
                    tokenProvider = sessionService,
                    appDataWithoutAuthorizationService = appDataWithoutAuthorizationService,
                )
            val logoutInterceptor = LogoutInterceptor(tokenProvider = sessionService)

            val authenticatedApiClient =
                ApiClient(
                    environment = environment,
                    interceptors = listOf(authInterceptor, logoutInterceptor, refreshTokenInterceptor),
                )
            val authRepository =
                DefaultAuthRepository(
                    apiClient = authenticatedApiClient,
                    tokenProvider = sessionService,
                )
            val appDataWithAuthorizationService = AppDataWithAuthorizationService(authRepository = authRepository)

            InjectionValues.register(
                FeatureToggle::class.java,
                FeatureToggle(FeatureRolloutStage.RELEASE),
            )

            current =
                AppGraph(
                    environment = environment,
                    sessionService = sessionService,
                    appDataWithoutAuthorizationService = appDataWithoutAuthorizationService,
                    appDataWithAuthorizationService = appDataWithAuthorizationService,
                    bareApiClient = bareApiClient,
                    authenticatedApiClient = authenticatedApiClient,
                    authRepository = authRepository,
                )
        }
    }
}
