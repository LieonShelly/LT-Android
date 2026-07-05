package com.littlethingsandroidai.app.home

import androidx.viewpager2.widget.ViewPager2
import com.littlethingsandroidai.core.uicomponent.LtHomeTabBar
import com.littlethingsandroidai.domain.coordinator.HomeRoute
import com.littlethingsandroidai.domain.coordinator.PreHomeRoute
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeCoordinatorTest {

    private val tabs =
        listOf(
            HomeRoute.CALENDAR,
            HomeRoute.THREAD,
            HomeRoute.INSIGHTS,
            HomeRoute.USER,
        )

    @Test
    fun push_thread_selectsIndex1() {
        val viewPager = mock<ViewPager2>()
        val tabBar = mock<LtHomeTabBar>()
        val coordinator = HomeCoordinator(viewPager, tabBar, tabs)

        coordinator.push(HomeRoute.THREAD)

        verify(viewPager).setCurrentItem(1, false)
        verify(tabBar).setSelectedIndex(1)
    }

    @Test
    fun popToRoot_selectsCalendar() {
        val viewPager = mock<ViewPager2>()
        val tabBar = mock<LtHomeTabBar>()
        val coordinator = HomeCoordinator(viewPager, tabBar, tabs)

        coordinator.popToRoot()

        verify(viewPager).setCurrentItem(0, false)
        verify(tabBar).setSelectedIndex(0)
    }

    @Test
    fun push_unknownRoute_doesNothing() {
        val viewPager = mock<ViewPager2>()
        val tabBar = mock<LtHomeTabBar>()
        val coordinator = HomeCoordinator(viewPager, tabBar, tabs)

        coordinator.push(PreHomeRoute.LOGIN)

        verify(viewPager, never()).setCurrentItem(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }
}
