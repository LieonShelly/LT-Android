package com.littlethingsandroidai.domain.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.littlethingsandroidai.R
import com.littlethingsandroidai.domain.calendar.model.CalendarMonth
import com.littlethingsandroidai.domain.calendar.model.MonthItemType
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class MonthPickerItem {
    data class YearHeader(val year: Int) : MonthPickerItem()

    data class MonthEntry(val month: CalendarMonth) : MonthPickerItem()
}

object MonthPickerItemsBuilder {
    fun from(months: List<CalendarMonth>): List<MonthPickerItem> =
        months.mapNotNull { month ->
            when (month.itemType) {
                MonthItemType.YEAR_PLACEHOLDER -> MonthPickerItem.YearHeader(month.date.year)
                MonthItemType.NORMAL -> MonthPickerItem.MonthEntry(month)
            }
        }
}

class CalendarMonthPickerAdapter(
    private val onMonthSelected: (CalendarMonth) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val monthTitleFormatter =
        DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

    private var items: List<MonthPickerItem> = emptyList()
    private var selectedMonthId: String? = null

    fun submitItems(items: List<MonthPickerItem>, selectedMonthId: String?) {
        this.items = items
        this.selectedMonthId = selectedMonthId
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        when (items[position]) {
            is MonthPickerItem.YearHeader -> VIEW_TYPE_YEAR
            is MonthPickerItem.MonthEntry -> VIEW_TYPE_MONTH
        }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_YEAR -> {
                val view = inflater.inflate(R.layout.item_calendar_month_picker_year, parent, false)
                YearViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_calendar_month_picker_month, parent, false)
                MonthViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (val item = items[position]) {
            is MonthPickerItem.YearHeader -> (holder as YearViewHolder).bind(item.year)
            is MonthPickerItem.MonthEntry -> {
                (holder as MonthViewHolder).bind(
                    month = item.month,
                    isSelected = item.month.id == selectedMonthId,
                    onMonthSelected = onMonthSelected,
                )
            }
        }
    }

    private class YearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView as TextView

        fun bind(year: Int) {
            title.text = year.toString()
        }
    }

    private class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView as TextView
        private val monthTitleFormatter =
            DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())

        fun bind(
            month: CalendarMonth,
            isSelected: Boolean,
            onMonthSelected: (CalendarMonth) -> Unit,
        ) {
            title.text = monthTitleFormatter.format(month.date)
            title.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isSelected) android.R.color.black else android.R.color.darker_gray,
                ),
            )
            title.paint.isFakeBoldText = isSelected
            title.setOnClickListener { onMonthSelected(month) }
        }
    }
    private companion object {
        const val VIEW_TYPE_YEAR = 0
        const val VIEW_TYPE_MONTH = 1
    }
}
