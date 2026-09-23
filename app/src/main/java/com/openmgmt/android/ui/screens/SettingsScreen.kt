package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.BuildConfig
import com.openmgmt.android.OpenMgmtApp
import com.openmgmt.android.ui.components.PageHeader
import com.openmgmt.android.ui.components.Section

/** Settings: account, sync server, and app info. */
@Composable
fun SettingsScreen(app: OpenMgmtApp) {
    var signedIn by remember { mutableStateOf(app.authManager.isSignedIn()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            PageHeader(
                eyebrow = "OPERATIONS",
                title = "Settings",
                description = "Account, sync, and app preferences.",
            )
        }
        item {
            Section("Black Candle account") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (signedIn) "Signed in" else "Not signed in",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Sign-in is required once so this device can register " +
                                "with the sync server.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (signedIn) {
                                OutlinedButton(onClick = {
                                    app.authManager.signOut()
                                    signedIn = false
                                }) { Text("Sign out") }
                            } else {
                                Text(
                                    "Use the Sync page to sign in.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Section("Sync server") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "https://openmgmt.blackcandletech.com",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "Production sync server",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Section("About") {
                Text(
                    "OpenMGMT ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
