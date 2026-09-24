package com.openmgmt.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openmgmt.android.ui.SyncViewModel
import com.openmgmt.android.ui.components.LocalContentGutter
import com.openmgmt.android.ui.theme.DrawerColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Sync page: mirrors the desktop Sync page (sign in / sync now / sign out). */
@Composable
fun SyncScreen(sync: SyncViewModel, pendingChanges: Int) {
    val state by sync.state.collectAsState()
    val busy = state.activity != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LocalContentGutter.current, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoCard {
            StatusRow(
                label = "Black Candle account",
                value = if (state.signedIn) "Signed in" else "Not signed in",
                ok = state.signedIn,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusRow(
                label = "This device",
                value = if (state.registered) "Registered" else "Not registered",
                ok = state.registered,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusRow(
                label = "Last sync",
                value = state.lastSyncedAt?.let(::formatTime) ?: "Not this session",
                ok = state.lastSyncedAt != null,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusRow(
                label = "Changes to send",
                value = if (pendingChanges == 0) "None" else pendingChanges.toString(),
            )
        }

        if (state.message != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    state.message!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.canSync) {
            Button(
                onClick = sync::syncNow,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sync now") }
        }
        if (!state.signedIn) {
            val signInLabel = if (state.activity == SyncViewModel.Activity.SigningIn) "Waiting for browser…" else "Sign in"
            if (state.canSync) {
                OutlinedButton(onClick = sync::signIn, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(signInLabel)
                }
            } else {
                Button(onClick = sync::signIn, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(signInLabel)
                }
            }
            Text(
                "Sign in once with your Black Candle account so this device can register " +
                    "with the sync server. Your data stays on this device until then.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            OutlinedButton(onClick = sync::signOut, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }

        Text(
            "Server: ${sync.serverUrl}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun InfoCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { content() }
    }
}

@Composable
fun StatusRow(label: String, value: String, ok: Boolean? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (ok == false) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        if (ok != null) {
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(
                    if (ok) DrawerColors.statusDot else MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))
