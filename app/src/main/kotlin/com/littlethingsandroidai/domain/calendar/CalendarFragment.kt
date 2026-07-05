package com.littlethingsandroidai.domain.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.littlethingsandroidai.R
import com.littlethingsandroidai.domain.calendar.detail.ReflectionDetailFragment
import com.littlethingsandroidai.domain.calendar.model.Answer
import com.littlethingsandroidai.domain.calendar.model.Question
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
    private lateinit var monthPickerAdapter: CalendarMonthPickerAdapter
    private var isSyncingPager = false
    private var isMonthPickerVisible = false

    private val monthTitleFormatter =
        DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

    private val weekdayDefaultColor = Color.parseColor("#323232")
    private val weekdayMutedColor = Color.parseColor("#CDCDCD")

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

        monthPagerAdapter =
            CalendarMonthPagerAdapter(requireContext()) { answer ->
                openReflectionDetail(answer)
            }
        binding.monthViewPager.adapter = monthPagerAdapter
        binding.monthViewPager.registerOnPageChangeCallback(pageChangeCallback)

        monthPickerAdapter =
            CalendarMonthPickerAdapter { month ->
                viewModel.selectMonth(month)
                setMonthPickerVisible(false)
                syncPagerAdapterFromViewModel()
                scrollPagerToMonthIndex(viewModel.currentMonthIndex.value, smooth = true)
            }
        binding.monthPickerList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = monthPickerAdapter
            isNestedScrollingEnabled = true
        }

        setupHeader()
        setupSwipeRefresh()
        setupDetailContainer()
        setupTodayQuestion()
        observeViewModel()
        loadCalendarData()
    }

    private fun setupHeader() {
        binding.calendarHeader.todayBadge.text = LocalDate.now().dayOfMonth.toString()
        binding.calendarHeader.todayBadge.setOnClickListener {
            setMonthPickerVisible(false)
            viewModel.dismissMonthPicker()
            viewModel.scrollToCurrentMonth()
            syncPagerAdapterFromViewModel()
            scrollPagerToMonthIndex(viewModel.currentMonthIndex.value, smooth = true)
        }

        binding.calendarHeader.monthHeaderToggle.setOnClickListener {
            setMonthPickerVisible(!isMonthPickerVisible)
            viewModel.setMonthPickerVisible(isMonthPickerVisible)
        }
    }

    private fun setMonthPickerVisible(visible: Boolean) {
        isMonthPickerVisible = visible
        binding.monthPickerList.visibility = if (visible) View.VISIBLE else View.GONE
        binding.calendarHeader.monthChevron.rotation = if (visible) 180f else 0f
        binding.calendarSwipeRefresh.isEnabled = !visible
        if (visible) {
            updateMonthPicker()
            scrollMonthPickerToSelection()
        }
    }

    private fun scrollMonthPickerToSelection() {
        val selectedId = viewModel.currentMonth.value?.id ?: return
        val index =
            viewModel.monthPickerItems().indexOfFirst { item ->
                item is MonthPickerItem.MonthEntry && item.month.id == selectedId
            }
        if (index >= 0) {
            binding.monthPickerList.scrollToPosition(index)
        }
    }

    private fun setupTodayQuestion() {
        binding.todayQuestion.todayQuestionAddButton.setOnClickListener {
            showSubmitAnswerStub()
        }
        binding.todayQuestion.todayQuestionPrimary.setOnClickListener {
            showSubmitAnswerStub()
        }
        binding.todayQuestion.todayQuestionExpand.setOnClickListener {
            viewModel.toggleTodayQuestionExpanded()
        }
    }

    private fun showSubmitAnswerStub() {
        Toast.makeText(
            requireContext(),
            R.string.calendar_today_question_stub,
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun bindTodayQuestion(questions: List<Question>, expanded: Boolean) {
        val banner = binding.todayQuestion
        if (questions.isEmpty()) {
            banner.root.visibility = View.GONE
            return
        }

        banner.root.visibility = View.VISIBLE
        banner.todayQuestionPrimary.text = questions.first().title
        val hasMore = questions.size > 1
        banner.todayQuestionExpand.visibility = if (hasMore) View.VISIBLE else View.GONE
        banner.todayQuestionExpand.rotation = if (expanded) 180f else 0f

        banner.todayQuestionExtraList.removeAllViews()
        if (expanded && hasMore) {
            banner.todayQuestionExtraList.visibility = View.VISIBLE
            questions.drop(1).forEach { question ->
                val item =
                    TextView(requireContext()).apply {
                        text = question.title
                        setTextColor(Color.parseColor("#323232"))
                        textSize = 14f
                        setPadding(0, dp(8), 0, dp(8))
                        setOnClickListener { showSubmitAnswerStub() }
                    }
                banner.todayQuestionExtraList.addView(item)
            }
        } else {
            banner.todayQuestionExtraList.visibility = View.GONE
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun setupDetailContainer() {
        childFragmentManager.addOnBackStackChangedListener {
            val hasDetail = childFragmentManager.backStackEntryCount > 0
            binding.calendarDetailContainer.visibility =
                if (hasDetail) View.VISIBLE else View.GONE
        }
    }

    private fun openReflectionDetail(answer: Answer) {
        viewModel.markIconAsRead(answer)
        binding.calendarDetailContainer.visibility = View.VISIBLE
        childFragmentManager
            .beginTransaction()
            .replace(
                R.id.calendarDetailContainer,
                ReflectionDetailFragment.newInstance(answer),
                REFLECTION_DETAIL_TAG,
            )
            .addToBackStack(REFLECTION_DETAIL_TAG)
            .commit()
    }

    private fun setupSwipeRefresh() {
        binding.calendarSwipeRefresh.setColorSchemeResources(android.R.color.black)
        binding.calendarSwipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.refreshCurrentMonth()
                binding.calendarSwipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadCalendarData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generateMonths()
            viewModel.scrollToCurrentMonth()
            syncPagerAdapterFromViewModel()
            updateMonthPicker()
            scrollPagerToMonthIndex(viewModel.currentMonthIndex.value)
            viewModel.fetchData()
            viewModel.fetchTodayQuestions()
        }
    }

    private fun syncPagerAdapterFromViewModel() {
        monthPagerAdapter.updateMonths(viewModel.validMonths())
    }

    private fun updateMonthPicker() {
        monthPickerAdapter.submitItems(
            items = viewModel.monthPickerItems(),
            selectedMonthId = viewModel.currentMonth.value?.id,
        )
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
                        updateMonthPicker()
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
                        updateWeekdayRow(month.isFuture)
                        updateMonthPicker()
                    }
                }
                launch {
                    viewModel.currentMonthIndex.collect { index ->
                        if (binding.monthViewPager.currentItem != index) {
                            scrollPagerToMonthIndex(index)
                        }
                    }
                }
                launch {
                    viewModel.todayQuestions.collect { questions ->
                        bindTodayQuestion(questions, viewModel.todayQuestionExpanded.value)
                    }
                }
                launch {
                    viewModel.showTodayQuestion.collect { visible ->
                        binding.todayQuestion.root.visibility =
                            if (visible && viewModel.todayQuestions.value.isNotEmpty()) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
                }
                launch {
                    viewModel.todayQuestionExpanded.collect { expanded ->
                        bindTodayQuestion(viewModel.todayQuestions.value, expanded)
                    }
                }
            }
        }
    }

    private fun updateWeekdayRow(isFutureMonth: Boolean) {
        val color = if (isFutureMonth) weekdayMutedColor else weekdayDefaultColor
        with(binding.weekdayRow) {
            weekday0.setTextColor(color)
            weekday1.setTextColor(color)
            weekday2.setTextColor(color)
            weekday3.setTextColor(color)
            weekday4.setTextColor(color)
            weekday5.setTextColor(color)
            weekday6.setTextColor(color)
        }
    }

    override fun onDestroyView() {
        _binding?.monthViewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val REFLECTION_DETAIL_TAG = "reflection_detail"
    }
}
