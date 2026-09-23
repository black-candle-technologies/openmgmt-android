package com.openmgmt.android.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Minimal HTTP client for the OpenMGMT sync protocol (omgp/1).
 *
 * Endpoints:
 * - POST /omgp/v1/hello
 * - POST /omgp/v1/devices/register (Bearer <redacted> = OAuth access token)
 * - POST /omgp/v1/sync/push (device token in body)
 * - POST /omgp/v1/sync/pull (device token in body)
 */
class OmgpClient(
    private val baseUrl: String,
    private val bearerToken: String? = null,
    timeoutSeconds: Long = 30,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val http = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    private inline fun <reified Req, reified Res> post(path: String, body: Req): Res {
        val payload = json.encodeToString(body)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .post(payload)
            .apply {
                bearerToken?.let { header("Authorization", "Bearer <redacted>") }
            }
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body!!.string()
            if (!response.isSuccessful) {
                throw SyncException("HTTP ${response.code}: $text")
            }
            return json.decodeFromString(text)
        }
    }

    fun hello(request: HelloRequest): Unit =
        post<HelloRequest, Map<String, String>>("/omgp/v1/hello", request).let {}

    fun register(request: RegisterRequest): RegisterResponse =
        post("/omgp/v1/devices/register", request)

    fun push(request: PushRequest) {
        post<PushRequest, Map<String, String>>("/omgp/v1/sync/push", request)
    }

    fun pull(request: PullRequest): PullResponse =
        post("/omgp/v1/sync/pull", request)
}

class SyncException(message: String) : Exception(message)
