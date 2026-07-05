package com.littlethingsandroidai.domain.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.littlethingsandroidai.databinding.ItemCalendarDayCellBinding
import com.littlethingsandroidai.domain.calendar.model.Answer
import com.littlethingsandroidai.domain.calendar.model.CalendarDay

class CalendarDayGridAdapter(
    private val onStampClick: (Answer) -> Unit,
) : RecyclerView.Adapter<CalendarDayGridAdapter.DayViewHolder>() {

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
        return DayViewHolder(binding, onStampClick)
    }

    override fun onBindViewHolder(
        holder: DayViewHolder,
        position: Int,
    ) {
        holder.bind(days[position])
    }

    class DayViewHolder(
        private val binding: ItemCalendarDayCellBinding,
        private val onStampClick: (Answer) -> Unit,
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

            val answers = day.reflections?.reflections.orEmpty()
            when {
                day.isCurrentMonth && answers.isNotEmpty() -> {
                    binding.stampContainer.visibility = View.VISIBLE
                    binding.absentDash.visibility = View.GONE
                    CalendarStampBinder.bind(
                        container = binding.stampContainer,
                        answers = answers,
                        isCurrentMonth = day.isCurrentMonth,
                        onAnswerClick = onStampClick,
                    )
                }
                day.isAbsent && day.isCurrentMonth -> {
                    binding.stampContainer.visibility = View.GONE
                    binding.stampContainer.removeAllViews()
                    binding.absentDash.visibility = View.VISIBLE
                }
                else -> {
                    binding.stampContainer.visibility = View.GONE
                    binding.stampContainer.removeAllViews()
                    binding.absentDash.visibility = View.GONE
                }
            }
        }
    }
}
