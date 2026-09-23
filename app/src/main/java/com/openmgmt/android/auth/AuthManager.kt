package com.openmgmt.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val PREFS_FILE = "openmgmt_auth"
private const val KEY_ACCESS_TOKEN = "access_token"

/**
 * Native OAuth sign-in against the Black Candle auth service.
 *
 * Flow: [beginSignIn] opens the system browser (Custom Tabs) at the
 * authorization endpoint with PKCE S256; the redirect comes back to
 * [MainActivity] as a deep link and is forwarded to [handleRedirect];
 * the authorization code is then exchanged for an access token, which is
 * stored in EncryptedSharedPreferences (never in the app database).
 *
 * The access token is only used once: as the Bearer <redacted> device
 * registration. Sync itself runs on the device token.
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
    private var pending: PendingLogin? = null

    private data class PendingLogin(
        val verifier: String,
        val state: String,
        val result: CompletableDeferred<String>,
    )

    fun isSignedIn(): Boolean = prefs.contains(KEY_ACCESS_TOKEN)

    fun accessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /** Opens the system browser for sign-in. Completes when [handleRedirect] runs. */
    suspend fun beginSignIn() {
        val verifier = Pkce.codeVerifier()
        val state = Pkce.state()
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

        val deferred = CompletableDeferred<String>()
        mutex.withLock { pending = PendingLogin(verifier, state, deferred) }

        CustomTabsIntent.Builder().build().apply {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchUrl(context, authUrl)
        }

        val token = withTimeoutOrNull(5 * 60 * 1000L) { deferred.await() }
            ?: throw AuthException("Sign-in timed out waiting for the browser callback")
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    /**
     * Called from [MainActivity] when the OAuth redirect deep link arrives.
     * Validates state, exchanges the code, and resumes [beginSignIn].
     */
    fun handleRedirect(uri: Uri) {
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val current = pending ?: return
        pending = null
        when {
            error != null -> current.result.completeExceptionally(
                AuthException("Authorization failed: $error")
            )
            code.isNullOrEmpty() || state != current.state -> current.result.completeExceptionally(
                AuthException("Invalid OAuth callback")
            )
            else -> {
                try {
                    val token = exchangeCode(code, current.verifier)
                    current.result.complete(token)
                } catch (e: Exception) {
                    current.result.completeExceptionally(e)
                }
            }
        }
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
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
        pending = null
    }
}

class AuthException(message: String) : Exception(message)

fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ContextCompat.startActivity(this, intent, null)
}
