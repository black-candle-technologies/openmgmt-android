package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openmgmt.android.OpenMgmtApp
import kotlinx.coroutines.launch

/** Sync page: mirrors the desktop Sync page (sign in / sync now / sign out). */
@Composable
fun SyncScreen(app: OpenMgmtApp) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    val signedIn = remember(status) { app.authManager.isSignedIn() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (signedIn) "Signed in with your Black Candle account."
                    else "Not signed in.",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (status.isNotEmpty()) {
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(onClick = {
            scope.launch {
                status = "Waiting for browser…"
                runCatching { app.authManager.beginSignIn() }
                    .onSuccess { status = "Signed in. Your next sync will register this device." }
                    .onFailure { status = "Sign-in failed: ${it.message}" }
            }
        }) { Text("Sign in") }

        Button(onClick = {
            scope.launch {
                status = "Syncing…"
                runCatching { app.syncManager.syncOnce() }
                    .onSuccess { status = "Synced: ${it.pushed} pushed, ${it.pulled} pulled." }
                    .onFailure { status = "Sync failed: ${it.message}" }
            }
        }) { Text("Sync now") }

        Button(onClick = {
            app.authManager.signOut()
            status = "Signed out."
        }) { Text("Sign out") }
    }
}
