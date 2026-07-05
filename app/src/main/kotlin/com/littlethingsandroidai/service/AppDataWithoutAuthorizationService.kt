package com.littlethingsandroidai.service

import com.littlethingsandroidai.service.auth.repository.SessionDataRepositoryType
import com.littlethingsandroidai.service.auth.usecase.RefreshTokenUseCase
import com.littlethingsandroidai.service.auth.usecase.RefreshTokenUseCaseType

interface AppDataWithoutAuthorizationServiceful {
    val refreshTokenUseCase: RefreshTokenUseCaseType
}

class AppDataWithoutAuthorizationService(
    private val sessionDataRepository: SessionDataRepositoryType,
) : AppDataWithoutAuthorizationServiceful {
    override val refreshTokenUseCase: RefreshTokenUseCaseType by lazy {
        RefreshTokenUseCase(repository = sessionDataRepository)
    }
}
