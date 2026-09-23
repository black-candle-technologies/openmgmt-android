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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.BuildConfig
import com.openmgmt.android.ui.SyncViewModel
import com.openmgmt.android.ui.components.Section

/** Settings: account, sync server, and app info. */
@Composable
fun SettingsScreen(sync: SyncViewModel, onOpenSync: () -> Unit) {
    val state by sync.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                OutlinedButton(onClick = sync::signOut, modifier = Modifier.fillMaxWidth()) {
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
        Section("About") {
            InfoCard {
                StatusRow(label = "Version", value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            }
        }
    }
}
