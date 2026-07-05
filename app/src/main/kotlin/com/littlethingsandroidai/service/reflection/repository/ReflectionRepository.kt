package com.littlethingsandroidai.service.reflection.repository

import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.core.network.UniversalResponse
import com.littlethingsandroidai.domain.calendar.model.Answer
import com.littlethingsandroidai.domain.calendar.model.Category
import com.littlethingsandroidai.domain.calendar.model.DayReflections
import com.littlethingsandroidai.domain.calendar.model.Icon
import com.littlethingsandroidai.domain.calendar.model.Question
import com.littlethingsandroidai.service.reflection.dto.AnswerDto
import com.littlethingsandroidai.service.reflection.dto.CalendarDayDto
import com.littlethingsandroidai.service.reflection.request.ReflectionRequest
import java.time.LocalDate

interface ReflectionRepository {
    suspend fun fetchCalendarReflections(startMonth: LocalDate, endMonth: LocalDate): List<DayReflections>
}

class DefaultReflectionRepository(
    private val apiClient: ApiClient,
) : ReflectionRepository {
    override suspend fun fetchCalendarReflections(
        startMonth: LocalDate,
        endMonth: LocalDate,
    ): List<DayReflections> {
        val request =
            ReflectionRequest.Calendar(
                startDate = startMonth.toString(),
                endDate = endMonth.toString(),
            )
        val response = apiClient.sendRequest(request)
        val parsed: UniversalResponse<List<CalendarDayDto>> = response.parseJson()
        return parsed.data.map { dto ->
            DayReflections(
                day = LocalDate.parse(dto.date),
                reflections = dto.reflections.map(::mapAnswer),
            )
        }
    }

    private fun mapAnswer(dto: AnswerDto): Answer =
        Answer(
            id = dto.id,
            content = dto.content,
            question =
                dto.question?.let {
                    Question(
                        id = it.id,
                        title = it.title,
                        category = it.category?.let { c -> Category(id = c.id, name = c.name) },
                    )
                },
            icon =
                dto.icon?.let {
                    Icon(
                        id = it.id,
                        url = it.url,
                        status = it.status.orEmpty(),
                        readAt = it.readAt,
                    )
                },
        )
}
