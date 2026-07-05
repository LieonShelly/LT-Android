package com.littlethingsandroidai.domain.calendar

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.littlethingsandroidai.R
import com.littlethingsandroidai.domain.calendar.model.Answer
import com.littlethingsandroidai.domain.calendar.model.Icon
import kotlin.math.max
import kotlin.math.min

object CalendarStampBinder {

    private const val ICON_STATUS_GENERATED = "GENERATED"

    fun bind(
        container: FrameLayout,
        answers: List<Answer>,
        isCurrentMonth: Boolean,
        onAnswerClick: (Answer) -> Unit,
    ) {
        container.removeAllViews()
        if (answers.isEmpty()) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        container.alpha = if (isCurrentMonth) 1f else 0.5f

        container.post {
            val width = container.width
            val height = container.height
            if (width <= 0 || height <= 0) return@post

            when (answers.size) {
                1 -> layoutOneIcon(container, answers.first(), width, height, onAnswerClick)
                2 -> layoutTwoIcons(container, answers, width, height, onAnswerClick)
                3 -> layoutThreeIcons(container, answers, width, height, onAnswerClick)
                else -> layoutFourPlusIcons(container, answers, width, height, onAnswerClick)
            }
        }
    }

    private fun layoutOneIcon(
        container: FrameLayout,
        answer: Answer,
        width: Int,
        height: Int,
        onAnswerClick: (Answer) -> Unit,
    ) {
        val iconSize = min(width * 0.55f, height * 0.55f).toInt().coerceAtLeast(dp(container, 20))
        addStampIcon(container, answer, iconSize, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, dp(container, 4), onAnswerClick)
    }

    private fun layoutTwoIcons(
        container: FrameLayout,
        answers: List<Answer>,
        width: Int,
        height: Int,
        onAnswerClick: (Answer) -> Unit,
    ) {
        val horizontalPadding = dp(container, 6)
        val contentWidth = max(width - horizontalPadding * 2, 0)
        val contentHeight = max(height - dp(container, 23) - dp(container, 6), 0)
        val vSpacing = max(contentHeight * 0.04f, dp(container, 2).toFloat()).toInt()
        val iconSize = min(contentWidth * 0.64f, max((contentHeight - vSpacing) / 2f, 0f)).toInt()
        val xOffset = min(contentWidth * 0.16f, iconSize * 0.45f).toInt()

        val column =
            LinearLayout(container.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
        val params =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                topMargin = dp(container, 23)
                bottomMargin = dp(container, 6)
                marginStart = horizontalPadding
                marginEnd = horizontalPadding
            }
        container.addView(column, params)

        addStampToColumn(column, answers[0], iconSize, xOffset, onAnswerClick)
        if (vSpacing > 0) {
            column.addView(View(container.context), LinearLayout.LayoutParams(0, vSpacing))
        }
        addStampToColumn(column, answers[1], iconSize, -xOffset, onAnswerClick)
    }

    private fun layoutThreeIcons(
        container: FrameLayout,
        answers: List<Answer>,
        width: Int,
        height: Int,
        onAnswerClick: (Answer) -> Unit,
    ) {
        val horizontalPadding = dp(container, 6)
        val contentWidth = max(width - horizontalPadding * 2, 0)
        val contentHeight = max(height - dp(container, 23) - dp(container, 6), 0)
        val vSpacing = max(contentHeight * 0.04f, dp(container, 2).toFloat()).toInt()
        val iconSize = min(contentWidth * 0.62f, max((contentHeight - vSpacing * 2) / 3f, 0f)).toInt()
        val xOffset = min(contentWidth * 0.16f, iconSize * 0.45f).toInt()

        val column =
            LinearLayout(container.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
        val params =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                topMargin = dp(container, 23)
                bottomMargin = dp(container, 6)
                marginStart = horizontalPadding
                marginEnd = horizontalPadding
            }
        container.addView(column, params)

        addStampToColumn(column, answers[0], iconSize, xOffset, onAnswerClick)
        column.addView(View(container.context), LinearLayout.LayoutParams(0, vSpacing))
        addStampToColumn(column, answers[1], iconSize, -xOffset, onAnswerClick)
        column.addView(View(container.context), LinearLayout.LayoutParams(0, vSpacing))
        addStampToColumn(column, answers[2], iconSize, xOffset, onAnswerClick)
    }

    private fun layoutFourPlusIcons(
        container: FrameLayout,
        answers: List<Answer>,
        width: Int,
        height: Int,
        onAnswerClick: (Answer) -> Unit,
    ) {
        val horizontalPadding = dp(container, 6)
        val contentWidth = max(width - horizontalPadding * 2, 0)
        val contentHeight = max(height - dp(container, 23) - dp(container, 6), 0)
        val vSpacing = max(contentHeight * 0.04f, dp(container, 2).toFloat()).toInt()
        val iconSize = min(contentWidth * 0.52f, max((contentHeight - vSpacing * 2) / 3f, 0f)).toInt()
        val hPadding = max((contentWidth - iconSize * 2) * 0.5f, 0f).toInt()

        val column =
            LinearLayout(container.context).apply {
                orientation = LinearLayout.VERTICAL
            }
        val params =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                topMargin = dp(container, 23)
                bottomMargin = dp(container, 6)
                marginStart = horizontalPadding
                marginEnd = horizontalPadding
            }
        container.addView(column, params)

        addStampRow(column, answers[0], iconSize, Gravity.START, hPadding, onAnswerClick)
        column.addView(View(container.context), LinearLayout.LayoutParams(0, vSpacing))
        addStampRow(column, answers[1], iconSize, Gravity.END, hPadding, onAnswerClick)
        column.addView(View(container.context), LinearLayout.LayoutParams(0, vSpacing))

        val lastRow =
            FrameLayout(container.context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, iconSize)
            }
        column.addView(lastRow)

        val lastAnswer = answers.last()
        val lastIcon = createStampImageView(container.context, lastAnswer, iconSize, onAnswerClick)
        lastRow.addView(
            lastIcon,
            FrameLayout.LayoutParams(iconSize, iconSize, Gravity.START).apply {
                marginStart = hPadding
            },
        )

        if (answers.size > 3) {
            val badge =
                TextView(container.context).apply {
                    text = container.context.getString(R.string.calendar_stamp_more_badge, answers.size - 3)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                    setTypeface(typeface, Typeface.ITALIC)
                    setTextColor(0xFF000000.toInt())
                }
            lastRow.addView(
                badge,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.END or Gravity.CENTER_VERTICAL,
                ),
            )
        }
    }

    private fun addStampRow(
        column: LinearLayout,
        answer: Answer,
        iconSize: Int,
        gravity: Int,
        horizontalPadding: Int,
        onAnswerClick: (Answer) -> Unit,
    ) {
        val row = FrameLayout(column.context)
        column.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, iconSize))
        val icon = createStampImageView(column.context, answer, iconSize, onAnswerClick)
        row.addView(
            icon,
            FrameLayout.LayoutParams(iconSize, iconSize, gravity).apply {
                marginStart = if (gravity == Gravity.START) horizontalPadding else 0
                marginEnd = if (gravity == Gravity.END) horizontalPadding else 0
            },
        )
    }

    private fun addStampToColumn(
        column: LinearLayout,
        answer: Answer,
        iconSize: Int,
        xOffset: Int,
        onAnswerClick: (Answer) -> Unit,
    ) {
        val row = FrameLayout(column.context)
        column.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, iconSize))
        val icon = createStampImageView(column.context, answer, iconSize, onAnswerClick)
        row.addView(
            icon,
            FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER_HORIZONTAL).apply {
                marginStart = xOffset
                marginEnd = -xOffset
            },
        )
    }

    private fun addStampIcon(
        container: FrameLayout,
        answer: Answer,
        iconSize: Int,
        gravity: Int,
        marginStart: Int,
        marginBottom: Int,
        onAnswerClick: (Answer) -> Unit,
    ) {
        val icon = createStampImageView(container.context, answer, iconSize, onAnswerClick)
        container.addView(
            icon,
            FrameLayout.LayoutParams(iconSize, iconSize, gravity).apply {
                this.marginStart = marginStart
                bottomMargin = marginBottom
            },
        )
    }

    private fun createStampImageView(
        context: Context,
        answer: Answer,
        size: Int,
        onAnswerClick: (Answer) -> Unit,
    ): ImageView =
        ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            contentDescription = null
            scaleType = ImageView.ScaleType.FIT_CENTER
            loadStampIcon(answer.icon)
            setOnClickListener { onAnswerClick(answer) }
        }

    private fun ImageView.loadStampIcon(icon: Icon?) {
        if (icon?.status == ICON_STATUS_GENERATED && !icon.url.isNullOrBlank()) {
            load(icon.url) {
                placeholder(R.drawable.ic_calendar_stamp_placeholder)
                error(R.drawable.ic_calendar_stamp_placeholder)
                crossfade(true)
            }
        } else {
            setImageResource(R.drawable.ic_calendar_stamp_placeholder)
        }
    }

    private fun dp(view: View, value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            view.resources.displayMetrics,
        ).toInt()
}
