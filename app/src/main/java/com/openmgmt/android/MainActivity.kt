package com.openmgmt.android

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.openmgmt.android.ui.MainViewModel
import com.openmgmt.android.ui.SyncViewModel
import com.openmgmt.android.ui.components.AppScaffold
import com.openmgmt.android.ui.components.LocalSnackbarHostState
import com.openmgmt.android.ui.components.TaskActionsHost
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

    private lateinit var syncViewModel: SyncViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // Transparent, not the default translucent white/black scrim: with
            // 3-button navigation (One UI's default) the scrim drew a band that
            // clashed with the paper background and cut the charcoal drawer off.
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // The app paints its own background behind the bar; icon contrast
            // follows the theme (and the drawer, see AppScaffold).
            window.isNavigationBarContrastEnforced = false
        }
        handleOAuthRedirect(intent)

        val app = application as OpenMgmtApp
        val viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(app.repository),
        )[MainViewModel::class.java]
        syncViewModel = ViewModelProvider(
            this,
            SyncViewModel.Factory(app),
        )[SyncViewModel::class.java]
        val sync = syncViewModel

        setContent {
            OpenMgmtTheme {
                var route by rememberSaveable { mutableStateOf(Destination.Dashboard.route) }
                val current = Destination.fromRoute(route)
                val navigate: (Destination) -> Unit = { route = it.route }
                val syncState by sync.state.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // Back from any page returns to the Dashboard before leaving the app.
                BackHandler(enabled = current != Destination.Dashboard) {
                    navigate(Destination.Dashboard)
                }

                CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                    AppScaffold(
                        current = current,
                        onNavigate = navigate,
                        sync = syncState,
                        onSync = sync::syncNow,
                        snackbarHostState = snackbarHostState,
                    ) {
                        TaskActionsHost(viewModel, snackbarHostState) {
                            when (current) {
                                Destination.Dashboard -> DashboardScreen(viewModel, onNavigate = navigate)
                                Destination.DailyOps -> DailyOpsScreen(viewModel)
                                Destination.Tasks -> TaskListScreen(viewModel)
                                Destination.Schedule -> ScheduleScreen(viewModel)
                                Destination.Projects -> ProjectsScreen(viewModel)
                                Destination.Organizations -> OrganizationsScreen(viewModel)
                                Destination.Board -> BoardScreen(viewModel)
                                Destination.Sync -> SyncScreen(
                                    sync,
                                    pendingChanges = viewModel.pendingChangeCount.collectAsState().value,
                                )
                                Destination.Settings -> SettingsScreen(
                                    sync,
                                    onOpenSync = { navigate(Destination.Sync) },
                                )
                            }
                        }
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
                    Log.w("OpenMGMT", "OAuth redirect failed", it)
                }
                // Cold start via the redirect: onCreate hasn't built the VM yet.
                if (::syncViewModel.isInitialized) syncViewModel.refreshAccount()
            }
        }
    }
}
