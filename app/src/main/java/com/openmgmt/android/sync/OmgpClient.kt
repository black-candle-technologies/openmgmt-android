package com.openmgmt.android.sync

import kotlinx.serialization.decodeFromString
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
 * - POST /omgp/v1/devices/register (Authorization: Bearer = OAuth access token)
 * - POST /omgp/v1/sync/push (device token in the body's auth context)
 * - POST /omgp/v1/sync/pull (device token in the body's auth context)
 *
 * Protocol-level failures come back as HTTP 200 with an `error` field;
 * they are raised as [ProtocolException] ([UnauthorizedException] when the
 * device token or account token was rejected).
 */
class OmgpClient(
    private val baseUrl: String,
    private val bearerToken: String? = null,
    timeoutSeconds: Long = 30,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val http = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    private inline fun <reified Req, reified Res> post(
        path: String,
        body: Req,
        withBearer: Boolean = false,
    ): Res {
        val payload = json.encodeToString(body)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .post(payload)
            .apply {
                if (withBearer) bearerToken?.let { header("Authorization", "Bearer $it") }
            }
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncException("Server returned HTTP ${response.code}")
            }
            return json.decodeFromString(text)
        }
    }

    fun hello(request: HelloRequest): HelloResponse {
        val response: HelloResponse = post("/omgp/v1/hello", request)
        checkError(response.error)
        if (!response.compatible) throw SyncException("Server does not support $PROTOCOL_VERSION")
        return response
    }

    /** Sends the account token; only registration needs it. */
    fun register(request: RegisterRequest): RegisterResponse {
        val response: RegisterResponse = post("/omgp/v1/devices/register", request, withBearer = true)
        checkError(response.error)
        if (!response.accepted || response.deviceToken.isNullOrEmpty()) {
            throw SyncException("Device registration was not accepted")
        }
        return response
    }

    fun push(request: PushRequest): PushResponse {
        val response: PushResponse = post("/omgp/v1/sync/push", request)
        checkError(response.error)
        return response
    }

    fun pull(request: PullRequest): PullResponse {
        val response: PullResponse = post("/omgp/v1/sync/pull", request)
        checkError(response.error)
        return response
    }

    private fun checkError(error: ProtocolError?) {
        error ?: return
        if (error.code == "unauthorized") throw UnauthorizedException(error.message)
        throw ProtocolException(error.code, error.message)
    }
}

open class SyncException(message: String) : Exception(message)

open class ProtocolException(val code: String, message: String) : SyncException(message)

class UnauthorizedException(message: String) : ProtocolException("unauthorized", message)
