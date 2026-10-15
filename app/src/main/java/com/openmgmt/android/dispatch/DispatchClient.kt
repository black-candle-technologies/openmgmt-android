package com.openmgmt.android.dispatch

import android.content.Context
import com.openmgmt.android.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DispatchConfig(context: Context) {
    private val prefs = context.getSharedPreferences("dispatch", 0)
    var baseUrl: String
        get() = prefs.getString("url", "https://dispatch.blackcandletech.com")!!
        set(value) {
            val url = value.trim().toHttpUrl()
            require(url.isHttps && url.username.isEmpty() && url.password.isEmpty()) { "Use an HTTPS URL without credentials" }
            prefs.edit().putString("url", url.toString().trimEnd('/')).apply()
        }
}

enum class AgentState(val wire: String, val label: String) {
    NONE("none", "None"), DISPATCHED("dispatched", "Dispatched"), RUNNING("running", "Running"),
    NEEDS_INPUT("needs_input", "Needs input"), PR_OPENED("pr_opened", "PR opened"), DONE("done", "Done"), FAILED("failed", "Failed");
    companion object { fun fromWire(value: String?) = entries.find { it.wire == value } ?: NONE }
}

/** The transport keeps unknown event/model fields, so service additions remain compatible. */
class DispatchClient(private val auth: AuthManager, private val config: DispatchConfig) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val http = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).followRedirects(false).build()
    private suspend fun request(path: String, body: JsonObject? = null, taskId: String? = null): JsonElement = withContext(Dispatchers.IO) {
        val token = auth.validAccessToken()
        val url = config.baseUrl.toHttpUrl().newBuilder().addPathSegments(path).apply {
            taskId?.let { addQueryParameter("task_id", it) }
        }.build()
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").apply {
            body?.let { post(json.encodeToString(JsonObject.serializer(), it).toRequestBody("application/json".toMediaType())) }
        }.build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Dispatch unavailable (HTTP ${response.code})" }
            val text = response.body?.string().orEmpty()
            if (text.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(text)
        }
    }
    suspend fun generate(body: JsonObject) = request("v1/prompts:generate", body)
    suspend fun models() = request("v1/models")
    suspend fun dispatch(body: JsonObject) = request("v1/dispatches", body)
    suspend fun get(id: String) = request("v1/dispatches/${segment(id)}")
    suspend fun forTask(taskId: String) = request("v1/dispatches", taskId = taskId)
    suspend fun message(id: String, text: String) = request("v1/dispatches/${segment(id)}/messages", buildJsonObject { put("message", text) })
    suspend fun cancel(id: String) = request("v1/dispatches/${segment(id)}/cancel", buildJsonObject {})
    private fun segment(id: String): String { require(id.matches(Regex("[A-Za-z0-9_-]+"))); return id }
}
