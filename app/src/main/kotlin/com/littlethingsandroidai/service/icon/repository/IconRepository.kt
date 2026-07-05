package com.littlethingsandroidai.service.icon.repository

import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.core.network.UniversalResponse
import com.littlethingsandroidai.domain.calendar.model.IconReadResult
import com.littlethingsandroidai.service.icon.dto.IconReadResultDto
import com.littlethingsandroidai.service.icon.dto.toDomain
import com.littlethingsandroidai.service.icon.request.IconRequest

interface IconRepository {
    suspend fun markIconRead(iconId: String): IconReadResult
}

class DefaultIconRepository(
    private val apiClient: ApiClient,
) : IconRepository {
    override suspend fun markIconRead(iconId: String): IconReadResult {
        val request = IconRequest.MarkRead(iconId)
        val response = apiClient.sendRequest(request)
        val parsed: UniversalResponse<IconReadResultDto> = response.parseJson()
        return parsed.data.toDomain()
    }
}
