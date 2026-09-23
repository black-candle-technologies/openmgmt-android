package com.openmgmt.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openmgmt.android.OpenMgmtApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Account + sync state shared by the top bar, the Sync page, and Settings.
 * Work runs in [viewModelScope] so leaving the Sync page mid sign-in or
 * mid sync doesn't cancel it.
 */
class SyncViewModel(private val app: OpenMgmtApp) : ViewModel() {

    enum class Activity { SigningIn, Syncing }

    data class State(
        val signedIn: Boolean = false,
        val registered: Boolean = false,
        val activity: Activity? = null,
        val message: String? = null,
        val isError: Boolean = false,
        val lastSyncedAt: Long? = null,
    ) {
        /** Syncing needs either an account token (to register) or a device token. */
        val canSync: Boolean get() = signedIn || registered
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    val serverUrl: String get() = app.syncManager.serverUrl

    init {
        refreshAccount()
    }

    fun refreshAccount() {
        _state.update {
            it.copy(
                signedIn = app.authManager.isSignedIn(),
                registered = app.syncManager.isRegistered(),
            )
        }
    }

    fun signIn() {
        if (_state.value.activity != null) return
        _state.update { it.copy(activity = Activity.SigningIn, message = "Waiting for browser…", isError = false) }
        viewModelScope.launch {
            val result = runCatching { app.authManager.beginSignIn() }
            refreshAccount()
            _state.update {
                it.copy(
                    activity = null,
                    message = result.fold(
                        onSuccess = { "Signed in. Your next sync will register this device." },
                        onFailure = { e -> "Sign-in failed: ${e.message}" },
                    ),
                    isError = result.isFailure,
                )
            }
        }
    }

    fun syncNow() {
        if (_state.value.activity != null || !_state.value.canSync) return
        _state.update { it.copy(activity = Activity.Syncing, message = "Syncing…", isError = false) }
        viewModelScope.launch {
            val result = runCatching { app.syncManager.syncOnce() }
            refreshAccount()
            _state.update {
                it.copy(
                    activity = null,
                    message = result.fold(
                        onSuccess = { r ->
                            buildString {
                                append("Synced: sent ${r.pushed}, received ${r.applied}.")
                                if (r.rejected > 0) append(" ${r.rejected} changes were rejected by the server.")
                            }
                        },
                        onFailure = { e -> "Sync failed: ${e.message}" },
                    ),
                    isError = result.isFailure || (result.getOrNull()?.rejected ?: 0) > 0,
                    lastSyncedAt = if (result.isSuccess) System.currentTimeMillis() else it.lastSyncedAt,
                )
            }
        }
    }

    /** Signs out and disconnects this device, so nothing syncs until the next sign-in. */
    fun signOut() {
        if (_state.value.activity != null) return
        app.authManager.signOut()
        viewModelScope.launch {
            app.syncManager.disconnect()
            refreshAccount()
            _state.update {
                it.copy(
                    activity = null,
                    message = "Signed out. This device won't sync until you sign in again.",
                    isError = false,
                    lastSyncedAt = null,
                )
            }
        }
    }

    class Factory(private val app: OpenMgmtApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SyncViewModel(app) as T
    }
}
