package com.littlethingsandroidai.core.network

import com.littlethingsandroidai.core.common.AppEnvironment

interface EndPoint {
    fun resolve(environment: AppEnvironment): String
}
