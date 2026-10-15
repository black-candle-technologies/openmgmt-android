package com.openmgmt.android.dispatch

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.openmgmt.android.OpenMgmtApp
import com.openmgmt.android.data.ProjectEntity
import com.openmgmt.android.data.TaskEntity
import com.openmgmt.android.ui.MainViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private fun JsonElement.field(key: String): String? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchSheet(task: TaskEntity, project: ProjectEntity?, viewModel: MainViewModel,
    onDismiss: () -> Unit, onSignIn: () -> Unit, onBindRepo: () -> Unit, onDispatched: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as OpenMgmtApp
    val client = remember { DispatchClient(app.authManager, DispatchConfig(context)) }
    val scope = rememberCoroutineScope()
    var prompt by rememberSaveable(task.id) { mutableStateOf("Implement: ${task.title}\n\n${task.notes}") }
    var models by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var model by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var offline by remember { mutableStateOf(false) }
    var uncertain by rememberSaveable(task.id) { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    val signedIn = app.authManager.isSignedIn()
    val hasRepo = !project?.repoOwner.isNullOrBlank() && !project?.repoName.isNullOrBlank()
    val sessionId = task.agentDispatchId
    var session by remember { mutableStateOf<JsonElement?>(null) }
    var followup by rememberSaveable { mutableStateOf("") }
    fun body() = buildJsonObject {
        put("task_id", task.id); put("title", task.title); put("notes", task.notes)
        put("repo_owner", project?.repoOwner); put("repo_name", project?.repoName); put("repo_branch", project?.repoBranch ?: "main")
        put("prompt", prompt); put("model", model)
    }
    suspend fun update(value: JsonElement) {
        session = value
        val id = value.field("id") ?: sessionId ?: error("Missing dispatch id")
        val state = AgentState.fromWire(value.field("state"))
        viewModel.updateAgent(task.id, state.wire, id, value.field("pr_url"))
    }
    LaunchedEffect(task.id, generation) {
        if (!signedIn || !hasRepo || sessionId != null) return@LaunchedEffect
        busy = true
        uncertain = true
        try {
            // Reconcile first: a successful POST may have lost its response or the app may have died.
            val existing = client.forTask(task.id)
            val sessions = validatedDispatches(existing)
            val active = sessions.firstOrNull { it.field("state") in setOf("dispatched", "running", "needs_input", "pr_opened") }
            if (active != null) { update(active); return@LaunchedEffect }
            uncertain = false
            error = null
            val response = client.models()
            models = (response as? JsonArray)?.toList() ?: (response as? JsonObject)?.get("models")?.jsonArray?.toList().orEmpty()
            model = models.firstOrNull()?.field("id").orEmpty()
            val generated = client.generate(body())
            prompt = generated.field("prompt") ?: error("Missing prompt")
            offline = false
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            offline = true
            if (uncertain) error = "Sessions could not be confirmed. Check your session before dispatching."
        } finally { busy = false }
    }
    LaunchedEffect(sessionId, signedIn, lifecycleOwner) {
        if (sessionId == null || !signedIn) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                try { update(client.get(sessionId)); error = null
                } catch (e: CancellationException) { throw e
                } catch (e: Exception) { error = "Service unavailable. Your local tasks are safe." }
                delay(15_000)
            }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (sessionId == null) "Dispatch agent" else "Agent session", style = MaterialTheme.typography.titleLarge)
            Text(task.title)
            if (!signedIn) {
                Text("Sign in to dispatch an agent.")
                Button(onClick = onSignIn) { Text("Sign in") }
            } else if (sessionId != null) {
                Text(AgentState.fromWire(session?.field("state") ?: task.agentState).label, style = MaterialTheme.typography.titleMedium)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                val events = (session as? JsonObject)?.get("events") as? JsonArray
                events?.forEach { event -> Text(event.field("message") ?: event.field("type") ?: "Activity") }
                Text(session?.field("latest_activity") ?: "Waiting for activity…")
                val pr = session?.field("pr_url") ?: task.agentPrUrl
                if (pr != null && pr.startsWith("https://")) OutlinedButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pr))) }
                }) { Text("Open PR") }
                OutlinedTextField(followup, { followup = it }, label = { Text("Follow-up") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !busy && followup.isNotBlank(), onClick = { scope.launch {
                        busy = true
                        try { client.message(sessionId, followup); followup = ""; error = null
                        } catch (e: CancellationException) { throw e
                        } catch (e: Exception) { error = "Could not send. Try again." } finally { busy = false }
                    } }) { Text("Send") }
                    OutlinedButton(enabled = !busy, onClick = { scope.launch {
                        busy = true
                        try { client.cancel(sessionId); update(client.get(sessionId))
                        } catch (e: CancellationException) { throw e
                        } catch (e: Exception) { error = "Could not stop the agent. Try again." } finally { busy = false }
                    } }) { Text("Stop") }
                }
            } else {
                if (!hasRepo) {
                    Text("Bind a repository to this task's project before dispatching.")
                    OutlinedButton(onClick = onBindRepo) { Text("Bind a repository") }
                }
                if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Drafting with MiniMax M3…") }
                if (offline) Text("Offline template draft — reconnect to generate and dispatch.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Prompt") }, modifier = Modifier.fillMaxWidth(), minLines = 4, enabled = !busy)
                TextButton(onClick = { generation++ }, enabled = !busy && hasRepo) { Text(if (uncertain) "Check session" else "Regenerate") }
                models.forEach { entry ->
                    val id = entry.field("id") ?: return@forEach
                    FilterChip(selected = model == id, onClick = { model = id }, label = { Text("${entry.field("provider").orEmpty()} · ${entry.field("model") ?: id}") })
                }
                Button(enabled = !busy && !offline && !uncertain && hasRepo && model.isNotBlank() && prompt.isNotBlank(), onClick = {
                    scope.launch {
                        busy = true
                        uncertain = true
                        try {
                            val result = client.dispatch(body())
                            val id = result.field("id") ?: error("Missing dispatch id")
                            viewModel.updateAgent(task.id, "dispatched", id, result.field("pr_url"))
                            onDispatched()
                        } catch (e: CancellationException) { throw e
                        } catch (e: Exception) { uncertain = true; error = "Dispatch could not be confirmed. Check your session before retrying." }
                        finally { busy = false }
                    }
                }) { Text("Dispatch") }
            }
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    }
}
