package com.littlethingsandroidai.app.home

import androidx.viewpager2.widget.ViewPager2
import com.littlethingsandroidai.domain.coordinator.Coordinator
import com.littlethingsandroidai.domain.coordinator.HomeRoute
import com.littlethingsandroidai.domain.coordinator.Route

class HomeCoordinator(
    private val viewPager: ViewPager2,
    private val tabs: List<HomeRoute>,
) : Coordinator {
    override fun push(route: Route) {
        if (route !is HomeRoute) return
        val targetIndex = tabs.indexOf(route)
        if (targetIndex >= 0) {
            viewPager.setCurrentItem(targetIndex, false)
        }
    }

    override fun pop() = Unit

    override fun popToRoot() {
        if (tabs.isNotEmpty()) {
            viewPager.setCurrentItem(0, false)
        }
    }
}
