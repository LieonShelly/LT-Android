package com.littlethingsandroidai.app.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.littlethingsandroidai.R
import com.littlethingsandroidai.app.observeSessionExpiration
import com.littlethingsandroidai.core.uicomponent.HomeTabItem
import com.littlethingsandroidai.core.uicomponent.R as UiR
import com.littlethingsandroidai.databinding.ActivityHomeBinding
import com.littlethingsandroidai.domain.coordinator.HomeRoute
import com.littlethingsandroidai.domain.home.HomeTabAdapter

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var coordinator: HomeCoordinator
    private lateinit var userHomeCoordinator: UserHomeCoordinator

    private val tabs =
        listOf(
            HomeRoute.CALENDAR,
            HomeRoute.THREAD,
            HomeRoute.INSIGHTS,
            HomeRoute.USER,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyNavigationBarInsets()
        setupViewPager()
        setupTabBar()
        coordinator = HomeCoordinator(binding.homeViewPager, binding.homeTabBar, tabs)
        coordinator.bind()
        coordinator.push(HomeRoute.CALENDAR)
        userHomeCoordinator = UserHomeCoordinator()
        observeSessionExpiration()
    }

    override fun onDestroy() {
        if (::coordinator.isInitialized) {
            coordinator.unbind()
        }
        super.onDestroy()
    }

    private fun applyNavigationBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeTabBar) { view, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = navBars.bottom)
            insets
        }
    }

    private fun setupViewPager() {
        binding.homeViewPager.adapter = HomeTabAdapter(this, tabs)
        binding.homeViewPager.isUserInputEnabled = false
        binding.homeViewPager.offscreenPageLimit = tabs.size - 1
    }

    private fun setupTabBar() {
        binding.homeTabBar.setTabs(
            listOf(
                HomeTabItem(
                    iconRes = UiR.drawable.ic_tab_calendar_placeholder,
                    contentDescription = getString(R.string.home_tab_calendar_desc),
                ),
                HomeTabItem(
                    iconRes = UiR.drawable.ic_tab_thread_placeholder,
                    contentDescription = getString(R.string.home_tab_thread_desc),
                ),
                HomeTabItem(
                    iconRes = UiR.drawable.ic_tab_insights_placeholder,
                    contentDescription = getString(R.string.home_tab_insights_desc),
                ),
                HomeTabItem(
                    iconRes = UiR.drawable.ic_tab_user_placeholder,
                    contentDescription = getString(R.string.home_tab_user_desc),
                ),
            ),
        )
    }
}
