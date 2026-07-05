package com.littlethingsandroidai.service.auth.usecase

import com.littlethingsandroidai.service.auth.repository.SessionDataRepositoryType

interface RefreshTokenUseCaseType {
    suspend fun execute()
}

class RefreshTokenUseCase(
    private val repository: SessionDataRepositoryType,
) : RefreshTokenUseCaseType {
    override suspend fun execute() {
        repository.refreshToken()
    }
}
