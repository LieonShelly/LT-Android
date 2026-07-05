package com.littlethingsandroidai.app.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.littlethingsandroidai.app.observeSessionExpiration
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

        binding.homeViewPager.adapter = HomeTabAdapter(this, tabs)
        TabLayoutMediator(binding.homeTabLayout, binding.homeViewPager) { tab, position ->
            tab.text = tabs[position].name.lowercase().replaceFirstChar(Char::titlecase)
        }.attach()

        coordinator = HomeCoordinator(binding.homeViewPager, tabs)
        userHomeCoordinator = UserHomeCoordinator()
        observeSessionExpiration()
    }
}
