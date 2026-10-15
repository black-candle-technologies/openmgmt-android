package com.openmgmt.android.dispatch

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.openmgmt.android.OpenMgmtApp
import com.openmgmt.android.ui.MainViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Local use never waits for this best-effort, per-session throttled refresh. */
@Composable
fun RefreshAgentsOnResume(viewModel: MainViewModel) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val tasks by viewModel.tasks.collectAsState()
    val latest by rememberUpdatedState(tasks)
    val app = context.applicationContext as OpenMgmtApp
    val client = remember { DispatchClient(app.authManager, DispatchConfig(context)) }
    DisposableEffect(owner, client, tasks.map { it.id to it.agentDispatchId }) {
        fun refresh() {
            scope.launch {
                if (!app.authManager.isSignedIn()) return@launch
                latest.filter { it.agentDispatchId != null && System.currentTimeMillis() - (it.agentUpdatedAt ?: 0) > 60_000 }
                    .forEach { task ->
                        try {
                            val response = client.get(task.agentDispatchId!!)
                            val obj = response as? kotlinx.serialization.json.JsonObject ?: return@forEach
                            val state = (obj["state"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                            val pr = (obj["pr_url"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeUnless { it == "null" }
                            viewModel.updateAgent(task.id, AgentState.fromWire(state).wire, task.agentDispatchId, pr)
                        } catch (e: CancellationException) { throw e
                        } catch (_: Exception) { /* Offline is not a local-task failure. */ }
                    }
            }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
