package com.littlethingsandroidai.service.icon.request

import com.littlethingsandroidai.core.network.ApiRequest
import com.littlethingsandroidai.core.network.EndPoint
import com.littlethingsandroidai.core.network.HttpMethod
import com.littlethingsandroidai.core.network.HttpPayload
import com.littlethingsandroidai.service.DefaultEndPoint

sealed class IconRequest : ApiRequest {

    data class MarkRead(
        private val iconId: String,
    ) : IconRequest() {
        override val endPoint: EndPoint =
            DefaultEndPoint.baseUrl(path = "/api/answers/icons/$iconId/read")
        override val method: HttpMethod = HttpMethod.POST
        override val payload: HttpPayload = HttpPayload.Empty
    }
}
