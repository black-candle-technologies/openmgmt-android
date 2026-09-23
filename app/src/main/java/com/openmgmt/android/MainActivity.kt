package com.openmgmt.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "tasks") {
                    composable("tasks") {
                        TaskListScreen(
                            onOpenSync = { navController.navigate("sync") },
                        )
                    }
                    composable("sync") {
                        SyncScreen(
                            app = application as OpenMgmtApp,
                            onBack = { navController.popBackStack() },
                        )
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
            (application as OpenMgmtApp).authManager.handleRedirect(uri)
        }
    }
}
