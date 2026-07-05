package com.littlethingsandroidai.service.reflection.request

import com.littlethingsandroidai.core.network.ApiRequest
import com.littlethingsandroidai.core.network.EndPoint
import com.littlethingsandroidai.core.network.HttpMethod
import com.littlethingsandroidai.core.network.HttpPayload
import com.littlethingsandroidai.service.DefaultEndPoint

sealed class ReflectionRequest : ApiRequest {

    data class Calendar(
        private val startDate: String,
        private val endDate: String,
    ) : ReflectionRequest() {
        override val endPoint: EndPoint = DefaultEndPoint.baseUrl(path = "/api/calendar-view")
        override val method: HttpMethod = HttpMethod.GET
        override val payload: HttpPayload =
            HttpPayload.UrlEncoding(
                params = listOf("start" to startDate, "end" to endDate),
            )
    }

    data object QuestionsOfToday : ReflectionRequest() {
        override val endPoint: EndPoint = DefaultEndPoint.baseUrl(path = "/api/questions-of-the-day")
        override val method: HttpMethod = HttpMethod.GET
        override val payload: HttpPayload = HttpPayload.Empty
    }
}
