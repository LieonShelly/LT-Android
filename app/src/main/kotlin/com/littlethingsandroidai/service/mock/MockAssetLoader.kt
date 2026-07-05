package com.littlethingsandroidai.service.mock

interface MockAssetLoader {
    fun readText(relativePath: String): String
}
