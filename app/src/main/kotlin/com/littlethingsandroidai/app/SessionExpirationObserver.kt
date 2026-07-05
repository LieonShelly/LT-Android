package com.littlethingsandroidai.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.littlethingsandroidai.service.interceptor.SessionEvent
import com.littlethingsandroidai.service.interceptor.SessionEvents
import kotlinx.coroutines.launch

fun AppCompatActivity.observeSessionExpiration() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            SessionEvents.events.collect { event ->
                when (event) {
                    SessionEvent.SessionExpired -> restartFromSplash()
                }
            }
        }
    }
}

private fun AppCompatActivity.restartFromSplash() {
    val intent =
        Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    startActivity(intent)
    finish()
}
