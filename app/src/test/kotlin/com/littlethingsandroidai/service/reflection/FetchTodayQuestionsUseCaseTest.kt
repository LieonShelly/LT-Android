package com.littlethingsandroidai.service.reflection

import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.service.reflection.repository.DefaultReflectionRepository
import com.littlethingsandroidai.service.reflection.usecase.FetchTodayQuestionsUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FetchTodayQuestionsUseCaseTest {
    private lateinit var server: MockWebServer
    private lateinit var useCase: FetchTodayQuestionsUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiClient =
            ApiClient(
                environment = AppEnvironment.DEV,
                baseUrlOverride = server.url("/").toString(),
            )
        useCase = FetchTodayQuestionsUseCase(DefaultReflectionRepository(apiClient))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun execute_parsesQuestionsOfTheDayResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "success": true,
                      "data": [
                        {
                          "id": "q1",
                          "title": "What warmed you today?",
                          "category": { "name": "Life" }
                        },
                        {
                          "id": "q2",
                          "title": "What are you grateful for?",
                          "category": { "name": "Gratitude" }
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result = useCase.execute()

        assertEquals(2, result.size)
        assertEquals("q1", result[0].id)
        assertEquals("What warmed you today?", result[0].title)
        assertEquals("Life", result[0].category?.name)
        assertEquals("/api/questions-of-the-day", server.takeRequest().path)
    }
}
