package com.littlethingsandroidai.service.reflection.usecase

import com.littlethingsandroidai.domain.calendar.model.DayReflections
import com.littlethingsandroidai.service.reflection.repository.ReflectionRepository
import java.time.LocalDate

interface CalendarReflectionsUseCaseType {
    suspend fun execute(startMonth: LocalDate, endMonth: LocalDate): List<DayReflections>
}

class CalendarReflectionsUseCase(
    private val repository: ReflectionRepository,
) : CalendarReflectionsUseCaseType {
    override suspend fun execute(startMonth: LocalDate, endMonth: LocalDate): List<DayReflections> =
        repository.fetchCalendarReflections(startMonth, endMonth)
}
