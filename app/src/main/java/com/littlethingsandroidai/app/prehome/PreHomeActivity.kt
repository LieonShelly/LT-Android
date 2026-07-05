package com.littlethingsandroidai.app.prehome

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.littlethingsandroidai.R
import com.littlethingsandroidai.app.observeSessionExpiration
import com.littlethingsandroidai.databinding.ActivityPrehomeBinding

class PreHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrehomeBinding
    private lateinit var coordinator: PreHomeCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrehomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.prehomeNavHost) as NavHostFragment
        coordinator = PreHomeCoordinator(navHostFragment.navController)
        observeSessionExpiration()
    }
}
