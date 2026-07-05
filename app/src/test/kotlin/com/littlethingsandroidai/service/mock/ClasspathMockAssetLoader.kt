package com.littlethingsandroidai.service.mock

class ClasspathMockAssetLoader : MockAssetLoader {
    override fun readText(relativePath: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(relativePath)) {
            "Missing test resource: $relativePath"
        }.bufferedReader().use { it.readText() }
}
