package com.littlethingsandroidai.app

import android.app.Application
import com.littlethingsandroidai.core.common.AppEnvironment

class LTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.build(applicationContext, AppEnvironment.DEV)
    }
}
