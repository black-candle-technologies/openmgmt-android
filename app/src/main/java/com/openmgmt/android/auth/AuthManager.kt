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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val PREFS_FILE = "openmgmt_auth"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_PENDING_VERIFIER = "pending_verifier"
private const val KEY_PENDING_STATE = "pending_state"

/**
 * Native OAuth sign-in against the Black Candle auth service.
 *
 * Flow: [beginSignIn] opens the system browser (Custom Tabs) at the
 * authorization endpoint with PKCE S256; the redirect comes back to
 * [MainActivity] as a deep link and is forwarded to [handleRedirect];
 * the authorization code is exchanged for an access token on
 * Dispatchers.IO, which is stored in EncryptedSharedPreferences
 * (never in the app database).
 *
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

    fun accessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /** Opens the system browser for sign-in. Returns when [handleRedirect] completes. */
    suspend fun beginSignIn() {
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
            .appendQueryParameter("client_id", config.clientId)
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
                    val token = withContext(Dispatchers.IO) { exchangeCode(code, verifier) }
                    prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
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

    private fun exchangeCode(code: String, verifier: String): String {
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", config.redirectUri)
            .add("client_id", config.clientId)
            .add("code_verifier", verifier)
            .build()
        val request = Request.Builder()
            .url("${config.issuer.trimEnd('/')}/oauth/token")
            .post(form)
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw AuthException("Token exchange failed: HTTP ${response.code}")
            }
            val token = JSONObject(response.body!!.string()).optString("access_token")
            if (token.isEmpty()) throw AuthException("Token endpoint returned no access token")
            return token
        }
    }

    fun signOut() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_STATE)
            .apply()
        waiter = null
    }
}

class AuthException(message: String) : Exception(message)
