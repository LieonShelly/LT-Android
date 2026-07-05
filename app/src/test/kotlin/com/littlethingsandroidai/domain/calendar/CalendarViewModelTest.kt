package com.littlethingsandroidai.domain.calendar

import com.littlethingsandroidai.service.AppDataWithAuthorizationServiceful
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCaseType
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CalendarViewModelTest {
    private val calendarUseCase: CalendarReflectionsUseCaseType = mock()
    private val service: AppDataWithAuthorizationServiceful = mock {
        whenever(it.calendarReflectionsUseCase).thenReturn(calendarUseCase)
    }

    @Test
    fun generateMonths_includesCurrentMonth() = runTest {
        val viewModel = CalendarViewModel(service)
        viewModel.generateMonths()

        val current = YearMonth.now()
        val validMonths = viewModel.months.value.filter { it.isValidMonth }
        assertTrue(validMonths.any { it.date == current })
    }

    @Test
    fun generateMonths_startsFrom2025January() = runTest {
        val viewModel = CalendarViewModel(service)
        viewModel.generateMonths()

        val validMonths = viewModel.months.value.filter { it.isValidMonth }
        assertTrue(validMonths.first().date >= YearMonth.of(2025, 1))
    }
}
