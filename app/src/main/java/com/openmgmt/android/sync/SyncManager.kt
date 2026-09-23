package com.openmgmt.android.sync

import android.content.Context
import android.provider.Settings
import com.openmgmt.android.auth.AuthManager
import com.openmgmt.android.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Orchestrates one sync run against the sync server:
 * hello → register (if needed) → push → pull.
 *
 * Device registration is the only step that needs the Black Candle account:
 * the OAuth access token goes out as a Bearer <redacted> exactly once, and the
 * server returns a device token used for all subsequent syncs. The device
 * token is stored in the local database; the OAuth token never is.
 */
class SyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val authManager: AuthManager,
    private val serverUrl: String = "https://openmgmt.blackcandletech.com",
) {
    data class SyncResult(
        val pushed: Int,
        val pulled: Int,
    )

    suspend fun syncOnce(): SyncResult = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("openmgmt_sync", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null)
            ?: UUID.randomUUID().toString().also {
                prefs.edit().putString("device_id", it).apply()
            }
        var deviceToken: String? = prefs.getString("device_token", null)

        // Registration needs the account Bearer <redacted> the first time (or after
        // the server rejects a stale device token with a re-register signal).
        if (deviceToken == null) {
            val accountToken = authManager.accessToken()
                ?: throw SyncException("Sign in with your Black Candle account first")
            val client = OmgpClient(serverUrl, bearerToken = accountToken)
            client.hello(HelloRequest(deviceId = deviceId))
            val registration = client.register(
                RegisterRequest(
                    deviceId = deviceId,
                    deviceName = android.os.Build.MODEL ?: "Android device",
                )
            )
            if (!registration.accepted || registration.deviceToken.isNullOrEmpty()) {
                throw SyncException(
                    registration.error?.message ?: "Device registration was not accepted"
                )
            }
            deviceToken = registration.deviceToken
            prefs.edit().putString("device_token", deviceToken).apply()
        }

        val client = OmgpClient(serverUrl)
        // TODO: collect unsynced local events from AppDatabase and push them.
        client.push(
            PushRequest(deviceId = deviceId, deviceToken = deviceToken!!)
        )
        val pull = client.pull(
            PullRequest(deviceId = deviceId, deviceToken = deviceToken!!)
        )
        // TODO: apply pull.events to AppDatabase and advance the cursor.

        SyncResult(pushed = 0, pulled = pull.events.size)
    }

    @Suppress("unused")
    private fun androidDeviceName(): String =
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: (android.os.Build.MODEL ?: "Android device")
}
