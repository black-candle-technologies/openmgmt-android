package com.openmgmt.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(app: OpenMgmtApp, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    val signedIn = remember(status) { app.authManager.isSignedIn() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sync") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (signedIn) "Signed in with your Black Candle account." else "Not signed in.")
            if (status.isNotEmpty()) Text(status)

            Button(onClick = {
                scope.launch {
                    status = "Waiting for browser…"
                    runCatching { app.authManager.beginSignIn() }
                        .onSuccess { status = "Signed in." }
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

            Button(onClick = onBack) { Text("Back") }
        }
    }
}
