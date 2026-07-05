package com.littlethingsandroidai.service.reflection.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarDayDto(
    val date: String,
    val reflections: List<AnswerDto> = emptyList(),
)

@Serializable
data class AnswerDto(
    val id: String,
    val content: String,
    @SerialName("created_ymd") val createdYmd: String? = null,
    val question: QuestionDto? = null,
    val icon: IconDto? = null,
)

@Serializable
data class QuestionDto(
    val id: String,
    val title: String,
    val category: CategoryDto? = null,
)

@Serializable
data class CategoryDto(
    val id: String? = null,
    val name: String,
)

@Serializable
data class IconDto(
    val id: String? = null,
    val url: String? = null,
    val status: String? = null,
    @SerialName("read_at") val readAt: String? = null,
)
