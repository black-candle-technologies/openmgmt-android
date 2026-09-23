package com.openmgmt.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val PREFS_FILE = "openmgmt_auth"
private const val KEY_CLIENT_ID = "dcr_client_id"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
private const val KEY_PENDING_VERIFIER = "pending_verifier"
private const val KEY_PENDING_STATE = "pending_state"

/** Refresh the access token this far before its reported expiry. */
private const val REFRESH_LEEWAY_MILLIS = 5 * 60 * 1000L

/**
 * Native OAuth sign-in against the Black Candle auth service (RFC 8252).
 *
 * The app is a public native client: on first sign-in it self-registers
 * via Dynamic Client Registration (RFC 7591) — no client secret, PKCE
 * S256 — and persists the issued client_id. The authorization flow opens
 * the system browser (Custom Tabs); the redirect comes back to
 * [MainActivity] as a deep link and is forwarded to [handleRedirect];
 * the authorization code is exchanged for an access token plus a
 * rotating refresh token on Dispatchers.IO.
 *
 * Tokens live in EncryptedSharedPreferences, never in the app database.
 * The pending PKCE verifier/state is persisted, not just held in memory,
 * so the flow survives the app process being killed while the browser is
 * in the foreground. The access token is only used once: as the Bearer
 * <redacted> device registration. Sync itself runs on the device token.
 */
class AuthManager(
    private val context: Context,
    private val config: OAuthConfig = OAuthConfig(),
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val mutex = Mutex()
    private var waiter: CompletableDeferred<Unit>? = null

    fun isSignedIn(): Boolean = prefs.contains(KEY_ACCESS_TOKEN)

    /** Opens the system browser for sign-in. Returns when [handleRedirect] completes. */
    suspend fun beginSignIn() {
        val clientId = withContext(Dispatchers.IO) { ensureClientId() }
        val verifier = Pkce.codeVerifier()
        val state = Pkce.state()
        // Persist before leaving the app: the process may die while the
        // browser is in the foreground.
        prefs.edit()
            .putString(KEY_PENDING_VERIFIER, verifier)
            .putString(KEY_PENDING_STATE, state)
            .apply()

        val authUrl = Uri.parse("${config.issuer.trimEnd('/')}/oauth/authorize")
            .buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", config.redirectUri)
            .appendQueryParameter("scope", config.scope)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", Pkce.codeChallenge(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

        val deferred = CompletableDeferred<Unit>()
        mutex.withLock { waiter = deferred }

        try {
            CustomTabsIntent.Builder().build().apply {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchUrl(context, authUrl)
            }

            withTimeoutOrNull(5 * 60 * 1000L) { deferred.await() }
                ?: throw AuthException("Sign-in timed out waiting for the browser callback")
        } finally {
            mutex.withLock { if (waiter === deferred) waiter = null }
        }
    }

    /**
     * Called from [MainActivity] when the OAuth redirect deep link arrives.
     * Must be called from a coroutine: the token exchange runs on
     * Dispatchers.IO. Returns true if the URI was an OAuth callback.
     * Throws [AuthException] if the callback carries an error or the
     * exchange fails.
     */
    suspend fun handleRedirect(uri: Uri): Boolean {
        val verifier = prefs.getString(KEY_PENDING_VERIFIER, null)
        val expectedState = prefs.getString(KEY_PENDING_STATE, null)
        if (verifier.isNullOrEmpty() || expectedState.isNullOrEmpty()) return false
        // Clear immediately: a captured redirect URI must not be replayable.
        prefs.edit()
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_STATE)
            .apply()

        val error = uri.getQueryParameter("error")
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")

        try {
            when {
                error != null -> throw AuthException("Authorization failed: $error")
                code.isNullOrEmpty() || state != expectedState ->
                    throw AuthException("Invalid OAuth callback")
                else -> {
                    val clientId = withContext(Dispatchers.IO) { ensureClientId() }
                    val tokens = withContext(Dispatchers.IO) {
                        exchangeCode(clientId, code, verifier)
                    }
                    storeTokens(tokens)
                }
            }
            mutex.withLock {
                waiter?.complete(Unit)
                waiter = null
            }
        } catch (e: Exception) {
            mutex.withLock {
                waiter?.completeExceptionally(e)
                waiter = null
            }
            throw e
        }
        return true
    }

    /**
     * Returns a usable access token, refreshing it first when expired.
     * Throws [AuthException] when there is no session or the refresh
     * token is rejected — the user must sign in again.
     */
    suspend fun validAccessToken(): String = mutex.withLock {
        val now = System.currentTimeMillis()
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        if (!token.isNullOrEmpty() && now < expiresAt - REFRESH_LEEWAY_MILLIS) {
            return token
        }
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null)
            ?: throw AuthException("Session expired — please sign in again")
        val clientId = prefs.getString(KEY_CLIENT_ID, null)
            ?: throw AuthException("Session expired — please sign in again")
        val tokens = withContext(Dispatchers.IO) { refreshTokens(clientId, refresh) }
        storeTokens(tokens)
        return tokens.accessToken
    }

    fun signOut() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRES_AT)
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_STATE)
            .apply()
        waiter = null
        // The DCR client_id is kept: it identifies this install and
        // carries no user state, so the next sign-in skips registration.
    }

    // ---- Dynamic Client Registration (RFC 7591) ----

    /** Returns the stored DCR client_id, registering this install first if needed. */
    private fun ensureClientId(): String {
        prefs.getString(KEY_CLIENT_ID, null)?.let { return it }
        val body = JSONObject()
            .put("client_name", "OpenMGMT Android")
            .put("redirect_uris", org.json.JSONArray().put(config.redirectUri))
            .put("token_endpoint_auth_method", "none")
            .put("grant_types", org.json.JSONArray().put("authorization_code").put("refresh_token"))
            .put("response_types", org.json.JSONArray().put("code"))
            .put("scope", config.scope)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${config.issuer.trimEnd('/')}/oauth/register")
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw AuthException("Client registration failed: HTTP ${response.code}")
            }
            val id = JSONObject(response.body!!.string()).optString("client_id")
            if (id.isEmpty() || !id.startsWith("bct_")) {
                throw AuthException("Client registration returned an invalid client_id")
            }
            prefs.edit().putString(KEY_CLIENT_ID, id).apply()
            return id
        }
    }

    // ---- token endpoint ----

    private data class TokenPair(
        val accessToken: String,
        val refreshToken: String?,
        val expiresInSecs: Long,
    )

    private fun storeTokens(tokens: TokenPair) {
        val editor = prefs.edit()
            .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            .putLong(
                KEY_TOKEN_EXPIRES_AT,
                System.currentTimeMillis() + tokens.expiresInSecs * 1000,
            )
        if (tokens.refreshToken.isNullOrEmpty()) {
            editor.remove(KEY_REFRESH_TOKEN)
        } else {
            editor.putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
        }
        editor.apply()
    }

    private fun parseTokenResponse(json: String): TokenPair {
        val obj = JSONObject(json)
        val access = obj.optString("access_token")
        if (access.isEmpty()) throw AuthException("Token endpoint returned no access token")
        return TokenPair(
            accessToken = access,
            refreshToken = obj.optString("refresh_token").ifEmpty { null },
            expiresInSecs = obj.optLong("expires_in", 3600),
        )
    }

    private fun exchangeCode(clientId: String, code: String, verifier: String): TokenPair {
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", config.redirectUri)
            .add("client_id", clientId)
            .add("code_verifier", verifier)
            .build()
        val request = Request.Builder()
            .url("${config.issuer.trimEnd('/')}/oauth/token")
            .post(form)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 401) {
                    // The registered id is unknown server-side (DB reset?);
                    // drop it so the next sign-in re-registers.
                    prefs.edit().remove(KEY_CLIENT_ID).apply()
                }
                throw AuthException("Token exchange failed: HTTP ${response.code}")
            }
            return parseTokenResponse(response.body!!.string())
        }
    }

    /**
     * Rotates the token pair. Single-use refresh tokens mean exactly one
     * in-flight refresh per install; callers hold [mutex].
     */
    private fun refreshTokens(clientId: String, refreshToken: String): TokenPair {
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", clientId)
            .build()
        val request = Request.Builder()
            .url("${config.issuer.trimEnd('/')}/oauth/token")
            .post(form)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // invalid_grant: expired, rotated-away, or reuse-detected.
                // The session is over — clear it so the UI prompts a
                // fresh sign-in instead of retrying a dead token.
                prefs.edit()
                    .remove(KEY_ACCESS_TOKEN)
                    .remove(KEY_REFRESH_TOKEN)
                    .remove(KEY_TOKEN_EXPIRES_AT)
                    .apply()
                throw AuthException("Session expired — please sign in again")
            }
            return parseTokenResponse(response.body!!.string())
        }
    }
}

class AuthException(message: String) : Exception(message)
