package com.littlethingsandroidai.domain.calendar

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.littlethingsandroidai.R
import com.littlethingsandroidai.databinding.ItemCalendarMonthPageBinding
import com.littlethingsandroidai.domain.calendar.model.CalendarMonth
import java.time.LocalDate
import java.time.YearMonth

class CalendarMonthPagerAdapter(
    private val context: Context,
) : RecyclerView.Adapter<CalendarMonthPagerAdapter.MonthViewHolder>() {

    private var months: List<CalendarMonth> = emptyList()

    fun updateMonths(months: List<CalendarMonth>) {
        this.months = months
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = months.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MonthViewHolder {
        val binding =
            ItemCalendarMonthPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MonthViewHolder,
        position: Int,
    ) {
        holder.bind(months[position])
    }

    inner class MonthViewHolder(
        private val binding: ItemCalendarMonthPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dayGridAdapter = CalendarDayGridAdapter()

        init {
            binding.dayGrid.apply {
                layoutManager = GridLayoutManager(context, 7)
                adapter = dayGridAdapter
                isNestedScrollingEnabled = false
            }
        }

        fun bind(month: CalendarMonth) {
            dayGridAdapter.submitList(month.days)
            bindFooter(month)
        }

        private fun bindFooter(month: CalendarMonth) {
            binding.monthFooter.text =
                if (month.date == YearMonth.now()) {
                    val today = LocalDate.now()
                    if (today == month.date.atEndOfMonth()) {
                        context.getString(
                            R.string.calendar_footer_current_last_day,
                            month.iconCount,
                        )
                    } else {
                        context.getString(
                            R.string.calendar_footer_current,
                            month.iconCount,
                            month.moreDaysToGo,
                        )
                    }
                } else {
                    context.getString(
                        R.string.calendar_footer_other,
                        month.iconCount,
                    )
                }
        }
    }
}
