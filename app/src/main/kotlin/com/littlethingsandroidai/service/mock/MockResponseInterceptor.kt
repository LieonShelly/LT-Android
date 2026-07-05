package com.littlethingsandroidai.service.mock

import com.littlethingsandroidai.core.common.log.LTLog
import com.littlethingsandroidai.core.network.UniversalResponse
import com.littlethingsandroidai.service.icon.dto.IconReadResultDto
import com.littlethingsandroidai.service.reflection.dto.CalendarDayDto
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockResponseInterceptor(
    private val assetLoader: MockAssetLoader,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method

        val body =
            when {
                method == "GET" && path == PATH_CALENDAR_VIEW -> handleCalendarView(request)
                method == "GET" && path == PATH_QUESTIONS_OF_THE_DAY ->
                    assetLoader.readText("mock/reflection/questions_of_the_day.json")
                method == "POST" && path.startsWith(PATH_MARK_READ_PREFIX) && path.endsWith("/read") ->
                    handleMarkRead(path)
                else -> {
                    LTLog.d(TAG, "Unmocked request: $method $path")
                    """{"success":false,"code":404,"data":null,"message":"Not mocked"}"""
                }
            }

        val isError = body.contains("\"success\":false")
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(if (isError) 404 else 200)
            .message(if (isError) "Not Found" else "OK")
            .body(body.toResponseBody(JSON_MEDIA_TYPE))
            .build()
    }

    private fun handleCalendarView(request: Request): String {
        val start = request.url.queryParameter("start")?.let(LocalDate::parse)
        val end = request.url.queryParameter("end")?.let(LocalDate::parse)
        require(start != null && end != null) { "calendar-view requires start and end" }

        val raw = assetLoader.readText("mock/calendar/calendar_view.json")
        val envelope = json.decodeFromString<UniversalResponse<List<CalendarDayDto>>>(raw)
        val filtered = MockCalendarViewFilter.filter(envelope.data, start, end)
        val filteredEnvelope = UniversalResponse(success = true, data = filtered)
        return json.encodeToString(
            UniversalResponse.serializer(ListSerializer(CalendarDayDto.serializer())),
            filteredEnvelope,
        )
    }

    private fun handleMarkRead(path: String): String {
        val iconId = path.removePrefix(PATH_MARK_READ_PREFIX).removeSuffix("/read")
        val readAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val envelope =
            UniversalResponse(
                success = true,
                data = IconReadResultDto(id = iconId, readAt = readAt),
            )
        return json.encodeToString(
            UniversalResponse.serializer(IconReadResultDto.serializer()),
            envelope,
        )
    }

    private companion object {
        const val TAG = "MockResponseInterceptor"
        const val PATH_CALENDAR_VIEW = "/api/calendar-view"
        const val PATH_QUESTIONS_OF_THE_DAY = "/api/questions-of-the-day"
        const val PATH_MARK_READ_PREFIX = "/api/answers/icons/"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
