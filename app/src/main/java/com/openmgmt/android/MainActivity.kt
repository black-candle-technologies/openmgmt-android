package com.openmgmt.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.openmgmt.android.ui.components.AppScaffold
import com.openmgmt.android.ui.nav.Destination
import com.openmgmt.android.ui.screens.PlaceholderPage
import com.openmgmt.android.ui.screens.SyncScreen
import com.openmgmt.android.ui.screens.TaskListScreen
import com.openmgmt.android.ui.theme.OpenMgmtTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthRedirect(intent)
        setContent {
            OpenMgmtTheme {
                val app = application as OpenMgmtApp
                var current by remember { mutableStateOf<Destination>(Destination.Dashboard) }

                AppScaffold(
                    current = current,
                    onNavigate = { current = it },
                    onRefresh = { /* TODO: refresh current page data */ },
                ) {
                    when (current) {
                        Destination.Dashboard -> PlaceholderPage("Dashboard")
                        Destination.DailyOps -> PlaceholderPage("Daily Operations")
                        Destination.Tasks -> TaskListScreen(
                            onOpenSync = { current = Destination.Sync },
                        )
                        Destination.Schedule -> PlaceholderPage("Schedule")
                        Destination.Projects -> PlaceholderPage("Projects")
                        Destination.Organizations -> PlaceholderPage("Organizations")
                        Destination.Board -> PlaceholderPage("Board")
                        Destination.Sync -> SyncScreen(app)
                        Destination.Settings -> PlaceholderPage("Settings")
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.openmgmt.android" && uri.host == "oauth2") {
            // Suspend: the token exchange runs on Dispatchers.IO. A failure
            // here is surfaced on the next Sync screen visit via isSignedIn().
            lifecycleScope.launch {
                runCatching {
                    (application as OpenMgmtApp).authManager.handleRedirect(uri)
                }.onFailure {
                    android.util.Log.w("OpenMGMT", "OAuth redirect failed", it)
                }
            }
        }
    }
}
