package com.littlethingsandroidai.service.icon.usecase

import com.littlethingsandroidai.domain.calendar.model.IconReadResult
import com.littlethingsandroidai.service.icon.repository.IconRepository

interface MarkIconReadUseCaseType {
    suspend fun execute(iconId: String): IconReadResult
}

class MarkIconReadUseCase(
    private val repository: IconRepository,
) : MarkIconReadUseCaseType {
    override suspend fun execute(iconId: String): IconReadResult =
        repository.markIconRead(iconId)
}
