package com.littlethingsandroidai.service.icon

import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.service.icon.repository.DefaultIconRepository
import com.littlethingsandroidai.service.icon.usecase.MarkIconReadUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MarkIconReadUseCaseTest {
    private lateinit var server: MockWebServer
    private lateinit var useCase: MarkIconReadUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiClient =
            ApiClient(
                environment = AppEnvironment.DEV,
                baseUrlOverride = server.url("/").toString(),
            )
        useCase = MarkIconReadUseCase(DefaultIconRepository(apiClient))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun execute_parsesMarkReadResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "success": true,
                      "data": {
                        "id": "icon1",
                        "read_at": "2026-07-05T10:00:00.000Z"
                      }
                    }
                    """.trimIndent(),
                ),
        )

        val result = useCase.execute("icon1")

        assertEquals("icon1", result.id)
        assertEquals("2026-07-05T10:00:00.000Z", result.readAt)
        assertEquals("/api/answers/icons/icon1/read", server.takeRequest().path)
    }
}
