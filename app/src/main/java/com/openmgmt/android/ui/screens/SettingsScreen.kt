package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.BuildConfig
import com.openmgmt.android.ui.SyncViewModel
import com.openmgmt.android.ui.components.LocalContentGutter
import com.openmgmt.android.ui.components.Section

/** Settings: account, sync server, and app info. */
@Composable
fun SettingsScreen(sync: SyncViewModel, onOpenSync: () -> Unit, onCheckUpdates: () -> Unit) {
    val state by sync.state.collectAsState()
    val context = LocalContext.current
    val config = remember { com.openmgmt.android.dispatch.DispatchConfig(context) }
    var url by remember { mutableStateOf(config.baseUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    val prefs = remember { context.getSharedPreferences("board", 0) }
    var preset by remember { mutableStateOf(prefs.getString("preset", "Balanced")!!) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LocalContentGutter.current, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Section("Account") {
            InfoCard {
                StatusRow(
                    label = "Black Candle account",
                    value = if (state.signedIn) "Signed in" else "Not signed in",
                    ok = state.signedIn,
                )
            }
            if (state.signedIn) {
                // Like the Sync page: no sign-out mid sign-in or mid sync.
                OutlinedButton(
                    onClick = sync::signOut,
                    enabled = state.activity == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign out")
                }
            } else {
                OutlinedButton(onClick = onOpenSync, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in on the Sync page")
                }
            }
        }
        Section("Sync server") {
            InfoCard {
                StatusRow(label = "Server", value = sync.serverUrl.removePrefix("https://"))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StatusRow(
                    label = "This device",
                    value = if (state.registered) "Registered" else "Not registered",
                    ok = state.registered,
                )
            }
        }
        Section("Board ranking preset") {
            listOf("Balanced", "Focus", "Deadline").forEach { option ->
                FilterChip(selected = preset == option, onClick = {
                    preset = option; prefs.edit().putString("preset", option).apply()
                }, label = { Text(option) })
            }
        }
        Section("Advanced") {
            OutlinedTextField(url, { url = it }, label = { Text("Dispatch server URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = {
                try { config.baseUrl = url; error = null } catch (e: IllegalArgumentException) { error = "Use a valid HTTPS URL without credentials" }
            }) { Text("Save server") }
        }
        Section("About") {
            InfoCard {
                StatusRow(label = "Version", value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                OutlinedButton(onClick = onCheckUpdates) { Text("Check for updates") }
            }
        }
    }
}
