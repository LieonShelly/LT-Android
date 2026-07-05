package com.littlethingsandroidai.core.uicomponent

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.littlethingsandroidai.core.uicomponent.databinding.ViewLtHomeTabBarBinding

class LtHomeTabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val binding = ViewLtHomeTabBarBinding.inflate(LayoutInflater.from(context), this)
    private val tabViews = mutableListOf<ImageView>()
    private var selectedIndex: Int = 0
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    fun setTabs(items: List<HomeTabItem>) {
        val container = binding.tabItemsContainer
        container.removeAllViews()
        tabViews.clear()

        items.forEachIndexed { index, item ->
            val tabView =
                ImageView(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                            if (index > 0) {
                                marginStart = dp(24)
                            }
                        }
                    setImageResource(item.iconRes)
                    contentDescription = item.contentDescription
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        if (selectedIndex != index) {
                            setSelectedIndex(index)
                            onTabSelectedListener?.invoke(index)
                        }
                    }
                }
            tabViews.add(tabView)
            container.addView(tabView)
        }
        updateSelectionVisuals()
    }

    fun setSelectedIndex(index: Int) {
        if (index !in tabViews.indices) {
            return
        }
        selectedIndex = index
        updateSelectionVisuals()
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    private fun updateSelectionVisuals() {
        val selectedColor = ContextCompat.getColor(context, R.color.lt_tab_bar_icon_selected)
        val unselectedColor = ContextCompat.getColor(context, R.color.lt_tab_bar_icon_unselected)
        tabViews.forEachIndexed { index, imageView ->
            imageView.setColorFilter(if (index == selectedIndex) selectedColor else unselectedColor)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
