package com.littlethingsandroidai.service.mock

import com.littlethingsandroidai.service.reflection.dto.CalendarDayDto
import java.time.LocalDate

object MockCalendarViewFilter {
    fun filter(
        days: List<CalendarDayDto>,
        start: LocalDate,
        end: LocalDate,
    ): List<CalendarDayDto> =
        days.filter { dto ->
            val day = LocalDate.parse(dto.date)
            !day.isBefore(start) && !day.isAfter(end)
        }
}
