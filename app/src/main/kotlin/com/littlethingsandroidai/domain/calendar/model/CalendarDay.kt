package com.littlethingsandroidai.domain.calendar.model

import java.time.LocalDate
import java.util.UUID

enum class DayType { PAST, TODAY, FUTURE }

data class CalendarDay(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    var isAbsent: Boolean,
    var reflections: DayReflections? = null,
) {
    val dayType: DayType
        get() {
            val today = LocalDate.now()
            return when {
                date.isBefore(today) -> DayType.PAST
                date.isEqual(today) -> DayType.TODAY
                else -> DayType.FUTURE
            }
        }

    fun copyWith(reflections: DayReflections): CalendarDay =
        copy(reflections = reflections, isAbsent = false)
}

data class DayReflections(
    val day: LocalDate,
    val reflections: List<Answer>,
)

data class Answer(
    val id: String,
    val content: String,
    val question: Question?,
    val icon: Icon?,
)

data class Icon(
    val id: String?,
    val url: String?,
    val status: String,
    val readAt: String?,
)

data class Question(
    val id: String,
    val title: String,
    val category: Category?,
)

data class Category(
    val id: String,
    val name: String,
)
