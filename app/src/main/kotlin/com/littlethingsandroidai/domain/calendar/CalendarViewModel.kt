package com.littlethingsandroidai.domain.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.littlethingsandroidai.domain.calendar.model.CalendarDay
import com.littlethingsandroidai.domain.calendar.model.CalendarMonth
import com.littlethingsandroidai.domain.calendar.model.MonthItemType
import com.littlethingsandroidai.core.common.log.LTLog
import com.littlethingsandroidai.domain.calendar.model.WeekDay
import com.littlethingsandroidai.service.AppDataWithAuthorizationServiceful
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val service: AppDataWithAuthorizationServiceful,
) : ViewModel() {

    private companion object {
        const val TAG = "CalendarViewModel"
    }

    val weekdays =
        listOf(
            WeekDay("S"),
            WeekDay("M"),
            WeekDay("T"),
            WeekDay("W"),
            WeekDay("T"),
            WeekDay("F"),
            WeekDay("S"),
        )

    private val _months = MutableStateFlow<List<CalendarMonth>>(emptyList())
    val months: StateFlow<List<CalendarMonth>> = _months.asStateFlow()

    private val _currentMonth = MutableStateFlow<CalendarMonth?>(null)
    val currentMonth: StateFlow<CalendarMonth?> = _currentMonth.asStateFlow()

    private val _currentMonthIndex = MutableStateFlow(0)
    val currentMonthIndex: StateFlow<Int> = _currentMonthIndex.asStateFlow()

    suspend fun generateMonths() {
        val startYearMonth = YearMonth.of(2025, 1)
        val endYearMonth = YearMonth.from(LocalDate.now().plusMonths(1))
        val generatedMonths = mutableListOf<CalendarMonth>()
        var current = startYearMonth

        while (current <= endYearMonth) {
            if (current.monthValue == 1 || current == startYearMonth) {
                generatedMonths.add(
                    CalendarMonth(
                        date = current,
                        itemType = MonthItemType.YEAR_PLACEHOLDER,
                    ),
                )
            }
            generatedMonths.add(generateSingleMonthData(current))
            current = current.plusMonths(1)
        }

        _months.value = generatedMonths
    }

    fun generateSingleMonthData(yearMonth: YearMonth): CalendarMonth {
        val firstDay = yearMonth.atDay(1)
        val today = LocalDate.now()
        val weekdayOffset = firstDay.dayOfWeek.value % 7
        val days = mutableListOf<CalendarDay>()

        for (index in 0 until weekdayOffset) {
            val date = firstDay.minusDays((weekdayOffset - index).toLong())
            days.add(
                CalendarDay(
                    date = date,
                    isCurrentMonth = false,
                    isToday = false,
                    isAbsent = true,
                ),
            )
        }

        val totalDays = yearMonth.lengthOfMonth()
        for (day in 1..totalDays) {
            val date = yearMonth.atDay(day)
            days.add(
                CalendarDay(
                    date = date,
                    isCurrentMonth = true,
                    isToday = date.isEqual(today),
                    isAbsent = true,
                ),
            )
        }

        return CalendarMonth(
            date = yearMonth,
            days = days,
            iconCount = 0,
            moreDaysToGo = 0,
            itemType = MonthItemType.NORMAL,
        )
    }

    fun validMonths(): List<CalendarMonth> = _months.value.filter { it.isValidMonth }

    fun scrollToCurrentMonth() {
        val todayYearMonth = YearMonth.now()
        val valid = validMonths()
        val month = valid.firstOrNull { it.date == todayYearMonth } ?: return
        _currentMonth.value = month
        _currentMonthIndex.value = valid.indexOf(month)
    }

    fun onMonthSelected(index: Int) {
        val valid = validMonths()
        if (index !in valid.indices) return
        val month = valid[index]
        _currentMonth.value = month
        _currentMonthIndex.value = index
        viewModelScope.launch {
            fetchData()
        }
    }

    suspend fun fetchData() {
        val current = _currentMonth.value ?: return
        val startOfMonth = current.date.atDay(1)
        val endOfMonth = current.date.atEndOfMonth()
        val reflections =
            try {
                service.calendarReflectionsUseCase.execute(
                    startMonth = startOfMonth,
                    endMonth = endOfMonth,
                )
            } catch (error: Exception) {
                LTLog.e(TAG, "fetchData failed for ${current.date}", error)
                return
            }

        var updatedMonths = _months.value.toMutableList()

        for (reflection in reflections) {
            for (monthIndex in updatedMonths.indices) {
                var days = updatedMonths[monthIndex].days.toMutableList()
                val dayIndex = days.indexOfFirst { it.date == reflection.day }
                if (dayIndex >= 0) {
                    days[dayIndex] = days[dayIndex].copyWith(reflection)
                }

                val firstAnswerDay = days.firstOrNull { it.reflections != null }?.date
                val lastAnswerDay = days.lastOrNull { it.reflections != null }?.date
                if (firstAnswerDay != null && lastAnswerDay != null) {
                    for (i in days.indices) {
                        if (days[i].reflections == null) {
                            days[i] =
                                days[i].copy(
                                    isAbsent =
                                        days[i].date >= firstAnswerDay &&
                                            days[i].date <= lastAnswerDay,
                                )
                        }
                    }
                }

                val totalIcons =
                    days
                        .filter { it.isCurrentMonth }
                        .flatMap { it.reflections?.reflections ?: emptyList() }
                        .count { it.icon != null }

                var updatedMonth =
                    updatedMonths[monthIndex].copy(
                        days = days,
                        iconCount = totalIcons,
                    )

                val today = LocalDate.now()
                if (updatedMonth.date == YearMonth.from(today)) {
                    val currentDayIndex = days.indexOfFirst { it.date == today }
                    if (currentDayIndex >= 0) {
                        updatedMonth =
                            updatedMonth.copy(
                                moreDaysToGo = days.size - currentDayIndex - 1,
                            )
                    }
                }

                updatedMonths[monthIndex] = updatedMonth
            }
        }

        _months.value = updatedMonths

        _currentMonth.value?.let { currentMonth ->
            updatedMonths.find { it.id == currentMonth.id }?.let { refreshed ->
                _currentMonth.value = refreshed
            }
        }
    }
}

class CalendarViewModelFactory(
    private val service: AppDataWithAuthorizationServiceful,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
