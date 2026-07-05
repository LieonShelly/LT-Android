package com.littlethingsandroidai.domain.calendar.model

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class MonthItemType { NORMAL, YEAR_PLACEHOLDER }

data class CalendarMonth(
    val id: String = UUID.randomUUID().toString(),
    val date: YearMonth,
    var days: List<CalendarDay> = emptyList(),
    var iconCount: Int = 0,
    var moreDaysToGo: Int = 0,
    val itemType: MonthItemType = MonthItemType.NORMAL,
) {
    val isFuture: Boolean
        get() = date.isAfter(YearMonth.from(LocalDate.now()))

    val isValidMonth: Boolean
        get() = itemType == MonthItemType.NORMAL
}
