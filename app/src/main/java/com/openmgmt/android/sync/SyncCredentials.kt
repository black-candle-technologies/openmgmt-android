package com.openmgmt.android.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * This install's sync identity: device id, the device token the server
 * issued at registration, and the account it was bound to. Kept in
 * EncryptedSharedPreferences because the device token is a credential.
 */
class SyncCredentials(private val context: Context) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "openmgmt_sync_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).also(::migrateLegacyPrefs)
    }

    val deviceId: String
        @Synchronized get() = prefs.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_DEVICE_ID, it).apply() }

    val deviceToken: String? get() = prefs.getString(KEY_DEVICE_TOKEN, null)
    val accountId: String? get() = prefs.getString(KEY_ACCOUNT_ID, null)
    val userId: String? get() = prefs.getString(KEY_USER_ID, null)

    fun isRegistered(): Boolean = deviceToken != null

    fun storeRegistration(deviceToken: String, accountId: String?, userId: String?) {
        prefs.edit()
            .putString(KEY_DEVICE_TOKEN, deviceToken)
            .putString(KEY_ACCOUNT_ID, accountId)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Forget the registration and start over as a new device. The server
     * binds a device id to one account and a different account can't claim
     * it without the old token, so a fresh id lets anyone sign in next.
     */
    @Synchronized
    fun reset() {
        prefs.edit()
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_ACCOUNT_ID)
            .remove(KEY_USER_ID)
            .putString(KEY_DEVICE_ID, UUID.randomUUID().toString())
            .apply()
    }

    /** Earlier builds kept the device id and token in plain prefs. */
    private fun migrateLegacyPrefs(secure: android.content.SharedPreferences) {
        val legacy = context.getSharedPreferences("openmgmt_sync", Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return
        secure.edit().apply {
            legacy.getString("device_id", null)?.let { putString(KEY_DEVICE_ID, it) }
            legacy.getString("device_token", null)?.let { putString(KEY_DEVICE_TOKEN, it) }
        }.apply()
        legacy.edit().clear().apply()
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_TOKEN = "device_token"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_USER_ID = "user_id"
    }
}
