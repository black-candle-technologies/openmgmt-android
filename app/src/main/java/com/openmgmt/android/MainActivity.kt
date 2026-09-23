package com.openmgmt.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.components.AppScaffold
import com.openmgmt.android.ui.nav.Destination
import com.openmgmt.android.ui.screens.BoardScreen
import com.openmgmt.android.ui.screens.DailyOpsScreen
import com.openmgmt.android.ui.screens.DashboardScreen
import com.openmgmt.android.ui.screens.OrganizationsScreen
import com.openmgmt.android.ui.screens.ProjectsScreen
import com.openmgmt.android.ui.screens.ScheduleScreen
import com.openmgmt.android.ui.screens.SettingsScreen
import com.openmgmt.android.ui.screens.SyncScreen
import com.openmgmt.android.ui.screens.TaskListScreen
import com.openmgmt.android.ui.theme.OpenMgmtTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthRedirect(intent)

        val app = application as OpenMgmtApp
        val viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(app.repository),
        )[MainViewModel::class.java]

        setContent {
            OpenMgmtTheme {
                var current by remember { mutableStateOf<Destination>(Destination.Dashboard) }

                AppScaffold(
                    current = current,
                    onNavigate = { current = it },
                    onRefresh = { /* TODO: refresh current page data */ },
                ) {
                    when (current) {
                        Destination.Dashboard -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { current = it },
                        )
                        Destination.DailyOps -> DailyOpsScreen(viewModel)
                        Destination.Tasks -> TaskListScreen(
                            viewModel = viewModel,
                            onOpenSync = { current = Destination.Sync },
                        )
                        Destination.Schedule -> ScheduleScreen(viewModel)
                        Destination.Projects -> ProjectsScreen(viewModel)
                        Destination.Organizations -> OrganizationsScreen(viewModel)
                        Destination.Board -> BoardScreen(viewModel)
                        Destination.Sync -> SyncScreen(app)
                        Destination.Settings -> SettingsScreen(app)
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
        // Must match OAuthConfig.redirectUri exactly: com.openmgmt.android:/oauth2/callback
        // (scheme only, no host — RFC 8252 §7.1).
        if (uri.scheme == "com.openmgmt.android" && uri.path == "/oauth2/callback") {
            // Suspend: the token exchange runs on Dispatchers.IO. A failure
            // here is surfaced on the next Sync screen visit via isSignedIn().
            lifecycleScope.launch {
                runCatching {
                    (application as OpenMgmtApp).authManager.handleRedirect(uri)
                }.onFailure {
                    Log.w("OpenMGMT", "OAuth redirect failed", it)
                }
            }
        }
    }
}
