package com.littlethingsandroidai.service.mock

import com.littlethingsandroidai.core.common.AppEnvironment
import com.littlethingsandroidai.core.network.ApiClient
import com.littlethingsandroidai.service.reflection.repository.DefaultReflectionRepository
import com.littlethingsandroidai.service.reflection.usecase.CalendarReflectionsUseCase
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockResponseInterceptorTest {
    private val assetLoader = ClasspathMockAssetLoader()
    private val interceptor = MockResponseInterceptor(assetLoader)

    @Test
    fun intercept_calendarView_filtersByQueryParams() = runBlocking {
        val client = apiClientWithMock()
        val useCase = CalendarReflectionsUseCase(DefaultReflectionRepository(client))
        val result =
            useCase.execute(
                startMonth = LocalDate.of(2026, 7, 1),
                endMonth = LocalDate.of(2026, 7, 31),
            )
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.day.monthValue == 7 })
    }

    @Test
    fun intercept_neverCallsProceed() {
        var proceedCalled = false
        val request =
            Request.Builder()
                .url("https://things.dvacode.tech/api/calendar-view?start=2026-07-01&end=2026-07-31")
                .get()
                .build()
        val response =
            interceptor.intercept(
                fakeChain(request) { req ->
                    proceedCalled = true
                    throw AssertionError("Must not proceed to network")
                },
            )
        assertEquals(200, response.code)
        assertTrue(response.body!!.string().contains("\"success\":true"))
        assertFalse(proceedCalled)
    }

    @Test
    fun intercept_markIconRead_returnsSuccessEnvelope() {
        val request =
            Request.Builder()
                .url("https://things.dvacode.tech/api/answers/icons/icon_demo_1/read")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .build()
        val response =
            interceptor.intercept(
                fakeChain(request) { throw AssertionError("no network") },
            )
        assertEquals(200, response.code)
        assertTrue(response.body!!.string().contains("icon_demo_1"))
    }

    private fun apiClientWithMock(): ApiClient =
        ApiClient(
            environment = AppEnvironment.DEV,
            interceptors = listOf(interceptor),
        )

    private fun fakeChain(
        request: Request,
        onProceed: (Request) -> Response,
    ): Interceptor.Chain =
        object : Interceptor.Chain {
            override fun request(): Request = request

            override fun proceed(req: Request): Response = onProceed(req)

            override fun connection() = null

            override fun call() = throw UnsupportedOperationException()

            override fun connectTimeoutMillis(): Int = 0

            override fun withConnectTimeout(
                timeout: Int,
                unit: TimeUnit,
            ): Interceptor.Chain = this

            override fun readTimeoutMillis(): Int = 0

            override fun withReadTimeout(
                timeout: Int,
                unit: TimeUnit,
            ): Interceptor.Chain = this

            override fun writeTimeoutMillis(): Int = 0

            override fun withWriteTimeout(
                timeout: Int,
                unit: TimeUnit,
            ): Interceptor.Chain = this
        }
}
