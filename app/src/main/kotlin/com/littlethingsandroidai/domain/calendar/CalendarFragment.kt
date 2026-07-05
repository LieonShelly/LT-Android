package com.littlethingsandroidai.domain.calendar

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.littlethingsandroidai.R
import com.littlethingsandroidai.app.AppGraph
import com.littlethingsandroidai.databinding.FragmentCalendarBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class CalendarFragment : Fragment(R.layout.fragment_calendar) {
    private var _binding: FragmentCalendarBinding? = null
    private val binding: FragmentCalendarBinding
        get() = requireNotNull(_binding)

    private val viewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(AppGraph.current.appDataWithAuthorizationService)
    }

    private lateinit var monthPagerAdapter: CalendarMonthPagerAdapter
    private var isSyncingPager = false

    private val monthTitleFormatter =
        DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

    private val pageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (isSyncingPager) return
                viewModel.onMonthSelected(position)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCalendarBinding.bind(view)

        monthPagerAdapter = CalendarMonthPagerAdapter(requireContext())
        binding.monthViewPager.adapter = monthPagerAdapter
        binding.monthViewPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.calendarHeader.todayBadge.text = LocalDate.now().dayOfMonth.toString()
        binding.calendarHeader.todayBadge.setOnClickListener {
            viewModel.scrollToCurrentMonth()
            syncPagerAdapterFromViewModel()
            scrollPagerToMonthIndex(viewModel.currentMonthIndex.value, smooth = true)
        }

        observeViewModel()
        loadCalendarData()
    }

    private fun loadCalendarData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generateMonths()
            viewModel.scrollToCurrentMonth()
            syncPagerAdapterFromViewModel()
            scrollPagerToMonthIndex(viewModel.currentMonthIndex.value)
            viewModel.fetchData()
        }
    }

    private fun syncPagerAdapterFromViewModel() {
        monthPagerAdapter.updateMonths(viewModel.validMonths())
    }

    private fun scrollPagerToMonthIndex(index: Int, smooth: Boolean = false) {
        if (index !in 0 until monthPagerAdapter.itemCount) return
        isSyncingPager = true
        binding.monthViewPager.setCurrentItem(index, smooth)
        isSyncingPager = false
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.months.collect { months ->
                        val validMonths = months.filter { it.isValidMonth }
                        val previousIndex = binding.monthViewPager.currentItem
                        monthPagerAdapter.updateMonths(validMonths)
                        if (validMonths.isNotEmpty() && previousIndex in validMonths.indices) {
                            scrollPagerToMonthIndex(previousIndex)
                        }
                    }
                }
                launch {
                    viewModel.currentMonth.collect { month ->
                        month ?: return@collect
                        binding.calendarHeader.monthTitle.text =
                            monthTitleFormatter.format(month.date)
                        binding.calendarHeader.yearTitle.text = month.date.year.toString()
                    }
                }
                launch {
                    viewModel.currentMonthIndex.collect { index ->
                        if (binding.monthViewPager.currentItem != index) {
                            scrollPagerToMonthIndex(index)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding?.monthViewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
        _binding = null
    }
}
