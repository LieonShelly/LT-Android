package com.littlethingsandroidai.service.icon.dto

import com.littlethingsandroidai.domain.calendar.model.IconReadResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IconReadResultDto(
    val id: String,
    @SerialName("read_at") val readAt: String,
)

fun IconReadResultDto.toDomain(): IconReadResult =
    IconReadResult(
        id = id,
        readAt = readAt,
    )
