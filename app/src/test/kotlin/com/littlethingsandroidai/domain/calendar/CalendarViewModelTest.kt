package com.littlethingsandroidai.domain.calendar

import com.littlethingsandroidai.domain.calendar.model.Question
import com.littlethingsandroidai.service.AppDataWithAuthorizationServiceful
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCaseType
import com.littlethingsandroidai.service.reflection.usecase.FetchTodayQuestionsUseCaseType
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CalendarViewModelTest {
    private val calendarUseCase: CalendarReflectionsUseCaseType = mock()
    private val todayQuestionsUseCase: FetchTodayQuestionsUseCaseType = mock()
    private val service: AppDataWithAuthorizationServiceful = mock {
        whenever(it.calendarReflectionsUseCase).thenReturn(calendarUseCase)
        whenever(it.fetchTodayQuestionsUseCase).thenReturn(todayQuestionsUseCase)
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

    @Test
    fun toggleMonthPicker_updatesVisibility() = runTest {
        val viewModel = CalendarViewModel(service)

        assertFalse(viewModel.isMonthPickerVisible.value)
        viewModel.toggleMonthPicker()
        assertTrue(viewModel.isMonthPickerVisible.value)
        viewModel.dismissMonthPicker()
        assertFalse(viewModel.isMonthPickerVisible.value)
    }

    @Test
    fun selectMonth_updatesCurrentMonthAndClosesPicker() = runTest {
        val viewModel = CalendarViewModel(service)
        viewModel.generateMonths()

        val target = viewModel.validMonths().last()
        viewModel.toggleMonthPicker()
        viewModel.selectMonth(target)

        assertEquals(target.id, viewModel.currentMonth.value?.id)
        assertFalse(viewModel.isMonthPickerVisible.value)
    }

    @Test
    fun monthPickerItems_includesYearHeadersAndMonths() = runTest {
        val viewModel = CalendarViewModel(service)
        viewModel.generateMonths()

        val items = viewModel.monthPickerItems()
        assertTrue(items.any { it is MonthPickerItem.YearHeader })
        assertTrue(items.any { it is MonthPickerItem.MonthEntry })
    }

    @Test
    fun organize_returnsHeadFirstThenRemainingQuestions() = runTest {
        whenever(todayQuestionsUseCase.execute()).thenReturn(
            listOf(
                Question(id = "q1", title = "First", category = null),
                Question(id = "q2", title = "Second", category = null),
            ),
        )
        val viewModel = CalendarViewModel(service)
        viewModel.fetchTodayQuestions()

        val organized = viewModel.organize()
        assertEquals(2, organized.size)
        assertEquals("q1", organized[0].id)
        assertEquals("q2", organized[1].id)
        assertTrue(viewModel.showTodayQuestion.value)
    }

    @Test
    fun validMonths_lastMonthIsFutureWhenNextMonthIncluded() = runTest {
        val viewModel = CalendarViewModel(service)
        viewModel.generateMonths()

        val lastMonth = viewModel.validMonths().last()
        assertTrue(lastMonth.date.isAfter(YearMonth.now()))
        assertTrue(lastMonth.isFuture)
    }
}
