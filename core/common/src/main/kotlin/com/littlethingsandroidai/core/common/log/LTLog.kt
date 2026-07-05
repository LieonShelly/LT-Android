package com.littlethingsandroidai.core.common.log

import android.util.Log

object LTLog {
    fun d(tag: String, message: String) = Log.d(tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        Log.e(tag, message, throwable)
}
