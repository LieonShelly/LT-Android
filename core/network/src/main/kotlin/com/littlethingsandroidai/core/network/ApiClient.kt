package com.littlethingsandroidai.core.network

import com.littlethingsandroidai.core.common.AppEnvironment
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class ApiClient(
    private val environment: AppEnvironment,
    private val baseUrlOverride: String? = null,
    interceptors: List<Interceptor> = emptyList(),
    maxRetry: Int = 2,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetry))
            .apply { interceptors.forEach(::addInterceptor) }
            .build()

    suspend fun sendRequest(request: ApiRequest): ApiResponse =
        withContext(Dispatchers.IO) {
            val resolvedUrl = request.endPoint.resolve(environment)
            val finalUrl = applyBaseUrlOverride(resolvedUrl)
            val okHttpRequest = buildRequest(finalUrl, request)

            try {
                client.newCall(okHttpRequest).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw AppNetworkException(
                            AppNetworkError.HttpError(
                                statusCode = response.code,
                                body = body.ifEmpty { null },
                            ),
                        )
                    }
                    ApiResponse(response.code, body, json)
                }
            } catch (ioe: IOException) {
                throw AppNetworkException(AppNetworkError.NetworkError(ioe))
            } catch (ane: AppNetworkException) {
                throw ane
            } catch (t: Throwable) {
                throw AppNetworkException(AppNetworkError.UnknownError(t))
            }
        }

    private fun applyBaseUrlOverride(rawUrl: String): String {
        val override = baseUrlOverride ?: return rawUrl
        val originUrl = rawUrl.toHttpUrlOrNull() ?: return override
        val overrideHttpUrl = override.toHttpUrlOrNull() ?: return rawUrl

        return overrideHttpUrl
            .newBuilder()
            .encodedPath(originUrl.encodedPath)
            .encodedQuery(originUrl.encodedQuery)
            .build()
            .toString()
    }

    private fun buildRequest(url: String, request: ApiRequest): Request {
        val httpUrl =
            url.toHttpUrlOrNull() ?: throw AppNetworkException(AppNetworkError.InvalidUrl(url))

        val requestBuilder = Request.Builder()
        val payload = request.payload
        when (request.method) {
            HttpMethod.GET -> {
                val finalUrl =
                    if (payload is HttpPayload.UrlEncoding) {
                        val builder = httpUrl.newBuilder()
                        payload.params.forEach { (key, value) ->
                            builder.addQueryParameter(key, value)
                        }
                        builder.build()
                    } else {
                        httpUrl
                    }
                requestBuilder.url(finalUrl).get()
            }

            else -> {
                val requestBody = toRequestBody(request.payload)
                requestBuilder.url(httpUrl)
                requestBuilder.method(request.method.name, requestBody)
            }
        }
        return requestBuilder.build()
    }

    private fun toRequestBody(payload: HttpPayload): RequestBody =
        when (payload) {
            HttpPayload.Empty -> ByteArray(0).toRequestBody(null, 0, 0)
            is HttpPayload.Json -> {
                val jsonObject = JsonObject(payload.body.mapValues { (_, value) -> value.toJsonElement() })
                val jsonText = json.encodeToString(JsonObject.serializer(), jsonObject)
                jsonText.toRequestBody("application/json; charset=utf-8".toMediaType())
            }

            is HttpPayload.UrlEncoding -> {
                val formBuilder = FormBody.Builder()
                payload.params.forEach { (key, value) -> formBuilder.add(key, value) }
                formBuilder.build()
            }
        }

}

private fun Any?.toJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(this.mapNotNull { (k, v) -> (k as? String)?.let { it to v.toJsonElement() } }.toMap())
        is List<*> -> JsonArray(this.map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

data class ApiResponse(
    val code: Int,
    val body: String,
    val json: Json = Json { ignoreUnknownKeys = true },
) {
    inline fun <reified T> parseJson(): UniversalResponse<T> {
        return try {
            json.decodeFromString<UniversalResponse<T>>(body)
        } catch (se: SerializationException) {
            throw AppNetworkException(AppNetworkError.SerializationError(se))
        } catch (t: Throwable) {
            throw AppNetworkException(AppNetworkError.UnknownError(t))
        }
    }
}
