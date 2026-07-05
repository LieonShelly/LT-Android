package com.littlethingsandroidai.service.mock

import android.content.Context

class AndroidMockAssetLoader(
    context: Context,
) : MockAssetLoader {
    private val assets = context.applicationContext.assets

    override fun readText(relativePath: String): String =
        assets.open(relativePath).bufferedReader().use { it.readText() }
}
