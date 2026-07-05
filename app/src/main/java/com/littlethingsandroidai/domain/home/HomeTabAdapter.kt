package com.littlethingsandroidai.domain.home

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.littlethingsandroidai.domain.coordinator.HomeRoute

class HomeTabAdapter(
    activity: FragmentActivity,
    private val tabs: List<HomeRoute>,
) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): PlaceholderTabFragment =
        PlaceholderTabFragment.newInstance(
            tabName = tabs[position].name.lowercase().replaceFirstChar(Char::titlecase),
        )
}
