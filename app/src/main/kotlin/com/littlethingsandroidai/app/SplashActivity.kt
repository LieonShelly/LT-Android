package com.littlethingsandroidai.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.littlethingsandroidai.databinding.ActivitySplashBinding
import com.littlethingsandroidai.app.home.HomeActivity
import com.littlethingsandroidai.app.prehome.PreHomeActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hasValidToken = AppGraph.current.sessionService.hasValidToken()
        val destination =
            if (hasValidToken) {
                HomeActivity::class.java
            } else {
                PreHomeActivity::class.java
            }

        startActivity(Intent(this, destination))
        finish()
    }
}
