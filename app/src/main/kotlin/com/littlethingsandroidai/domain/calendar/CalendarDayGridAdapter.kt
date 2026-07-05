package com.littlethingsandroidai.domain.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.littlethingsandroidai.databinding.ItemCalendarDayCellBinding
import com.littlethingsandroidai.domain.calendar.model.CalendarDay

class CalendarDayGridAdapter : RecyclerView.Adapter<CalendarDayGridAdapter.DayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun submitList(days: List<CalendarDay>) {
        this.days = days
        notifyDataSetChanged()
    }

    fun update(days: List<CalendarDay>) {
        submitList(days)
    }

    override fun getItemCount(): Int = days.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DayViewHolder {
        val binding =
            ItemCalendarDayCellBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DayViewHolder,
        position: Int,
    ) {
        holder.bind(days[position])
    }

    class DayViewHolder(
        private val binding: ItemCalendarDayCellBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(day: CalendarDay) {
            binding.dayNumber.text = day.date.dayOfMonth.toString()
            binding.dayNumber.setTextColor(
                if (day.isCurrentMonth) {
                    Color.parseColor("#323232")
                } else {
                    Color.parseColor("#CDCDCD")
                },
            )

            val hasReflections = day.reflections?.reflections?.isNotEmpty() == true
            when {
                hasReflections -> {
                    binding.stampPlaceholder.visibility = View.VISIBLE
                    binding.absentDash.visibility = View.GONE
                }
                day.isAbsent && day.isCurrentMonth -> {
                    binding.stampPlaceholder.visibility = View.GONE
                    binding.absentDash.visibility = View.VISIBLE
                }
                else -> {
                    binding.stampPlaceholder.visibility = View.GONE
                    binding.absentDash.visibility = View.GONE
                }
            }
        }
    }
}
