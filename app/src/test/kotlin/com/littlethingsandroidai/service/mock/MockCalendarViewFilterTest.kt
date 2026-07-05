package com.littlethingsandroidai.service.mock

import com.littlethingsandroidai.service.reflection.dto.CalendarDayDto
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockCalendarViewFilterTest {
    private val days =
        listOf(
            CalendarDayDto(date = "2026-07-01", reflections = emptyList()),
            CalendarDayDto(date = "2026-07-15", reflections = emptyList()),
            CalendarDayDto(date = "2026-08-01", reflections = emptyList()),
        )

    @Test
    fun filter_includesDaysWithinRange() {
        val result =
            MockCalendarViewFilter.filter(
                days = days,
                start = LocalDate.of(2026, 7, 1),
                end = LocalDate.of(2026, 7, 31),
            )
        assertEquals(2, result.size)
        assertEquals("2026-07-01", result[0].date)
        assertEquals("2026-07-15", result[1].date)
    }

    @Test
    fun filter_returnsEmptyWhenNoOverlap() {
        val result =
            MockCalendarViewFilter.filter(
                days = days,
                start = LocalDate.of(2026, 9, 1),
                end = LocalDate.of(2026, 9, 30),
            )
        assertTrue(result.isEmpty())
    }
}
