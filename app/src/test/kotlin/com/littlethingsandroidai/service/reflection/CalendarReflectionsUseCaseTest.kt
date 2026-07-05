package com.littlethingsandroidai.service.reflection

import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.service.reflection.repository.DefaultReflectionRepository
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCase
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalendarReflectionsUseCaseTest {
    private lateinit var server: MockWebServer
    private lateinit var useCase: CalendarReflectionsUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiClient =
            ApiClient(
                environment = AppEnvironment.DEV,
                baseUrlOverride = server.url("/").toString(),
            )
        useCase = CalendarReflectionsUseCase(DefaultReflectionRepository(apiClient))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun execute_parsesCalendarViewResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "success": true,
                      "data": [
                        {
                          "date": "2026-07-05",
                          "reflections": [
                            {
                              "id": "ans1",
                              "content": "A warm moment.",
                              "question": {
                                "id": "q1",
                                "title": "What warmed you today?",
                                "category": { "id": "c1", "name": "Life" }
                              },
                              "icon": {
                                "id": "icon1",
                                "url": "https://example.com/icon.webp",
                                "status": "GENERATED",
                                "read_at": null
                              }
                            }
                          ]
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
        )

        val result =
            useCase.execute(
                startMonth = LocalDate.of(2026, 7, 1),
                endMonth = LocalDate.of(2026, 7, 31),
            )

        assertEquals(1, result.size)
        assertEquals(LocalDate.of(2026, 7, 5), result[0].day)
        assertEquals("ans1", result[0].reflections[0].id)
        assertEquals("What warmed you today?", result[0].reflections[0].question?.title)
    }
}
