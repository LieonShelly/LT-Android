package com.littlethingsandroidai.app.home

import androidx.viewpager2.widget.ViewPager2
import com.littlethingsandroidai.core.uicomponent.LtHomeTabBar
import com.littlethingsandroidai.domain.coordinator.Coordinator
import com.littlethingsandroidai.domain.coordinator.HomeRoute
import com.littlethingsandroidai.domain.coordinator.Route

class HomeCoordinator(
    private val viewPager: ViewPager2,
    private val tabBar: LtHomeTabBar,
    private val tabs: List<HomeRoute>,
) : Coordinator {

    private val pageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabBar.setSelectedIndex(position)
            }
        }

    fun bind() {
        viewPager.isUserInputEnabled = false
        viewPager.registerOnPageChangeCallback(pageChangeCallback)
        tabBar.setOnTabSelectedListener { index ->
            if (viewPager.currentItem != index) {
                viewPager.setCurrentItem(index, false)
            }
        }
    }

    fun unbind() {
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
    }

    override fun push(route: Route) {
        if (route !is HomeRoute) return
        val targetIndex = tabs.indexOf(route)
        if (targetIndex >= 0) {
            viewPager.setCurrentItem(targetIndex, false)
            tabBar.setSelectedIndex(targetIndex)
        }
    }

    override fun pop() = Unit

    override fun popToRoot() {
        push(HomeRoute.CALENDAR)
    }
}
