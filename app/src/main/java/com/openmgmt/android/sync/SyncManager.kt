package com.openmgmt.android.sync

import com.openmgmt.android.BuildConfig
import com.openmgmt.android.auth.AuthManager
import com.openmgmt.android.data.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject

private const val PUSH_BATCH = 500
private const val PULL_LIMIT = 500
/** Safety valve so a misbehaving server can't keep a sync running forever. */
private const val MAX_PULL_PAGES = 200

/**
 * Orchestrates one sync run against the sync server, following the
 * desktop client (openmgmt-sync-client): hello → register (if needed) →
 * pull + replay → push.
 *
 * Pulling before pushing keeps conflicts consistent: remote changes are
 * replayed first, entities with unpushed local changes keep them, and then
 * those local changes are pushed, landing later in server order, which is
 * the order every device replays in.
 *
 * Device registration is the only step that needs the Black Candle account:
 * the OAuth access token goes out as a Bearer credential for registration
 * only, and the server returns a device token used for all syncs after
 * that. If the server stops recognizing the device token, the device
 * re-registers once per run, proving possession with the old token.
 */
class SyncManager(
    private val repository: MainRepository,
    private val authManager: AuthManager,
    private val credentials: SyncCredentials,
    val serverUrl: String = "https://openmgmt.blackcandletech.com",
) {
    data class SyncResult(
        val pushed: Int,
        val rejected: Int,
        val pulled: Int,
        val applied: Int,
    )

    private val running = Mutex()

    /** True once the server has issued this device a token. */
    fun isRegistered(): Boolean = credentials.isRegistered()

    /**
     * Sign-out: drop the device registration so nothing syncs until the
     * next sign-in, and queue a full re-push so whichever account signs in
     * next receives all local data.
     */
    suspend fun disconnect() {
        credentials.reset()
        repository.resetSyncState()
    }

    suspend fun syncOnce(): SyncResult = withContext(Dispatchers.IO) {
        if (!running.tryLock()) throw SyncException("A sync is already running")
        try {
            run()
        } finally {
            running.unlock()
        }
    }

    private suspend fun run(): SyncResult {
        val deviceId = credentials.deviceId
        var reregistered = false

        suspend fun register(previousToken: String?) {
            if (!authManager.isSignedIn()) {
                throw SyncException("Sign in to register this device")
            }
            val client = OmgpClient(serverUrl, bearerToken = authManager.validAccessToken())
            val registration = client.register(
                RegisterRequest(
                    deviceId = deviceId,
                    deviceName = android.os.Build.MODEL ?: "Android device",
                    previousDeviceToken = previousToken,
                    userHint = credentials.userId,
                )
            )
            credentials.storeRegistration(
                registration.deviceToken!!, registration.accountId, registration.userId,
            )
        }

        fun auth() = AuthContext(
            accountId = credentials.accountId,
            userId = credentials.userId,
            deviceId = deviceId,
            deviceToken = credentials.deviceToken,
        )

        /** Runs [call]; on a rejected device token, re-registers once and retries. */
        suspend fun <T> authorized(call: suspend () -> T): T = try {
            call()
        } catch (e: UnauthorizedException) {
            if (reregistered || !authManager.isSignedIn()) throw e
            reregistered = true
            register(previousToken = credentials.deviceToken)
            call()
        }

        val client = OmgpClient(serverUrl)
        client.hello(HelloRequest(clientVersion = BuildConfig.VERSION_NAME, deviceId = deviceId))
        if (!credentials.isRegistered()) register(previousToken = null)

        // Before pulling: pending snapshot events shield local rows from replay.
        repository.snapshotIfPending()

        var pulled = 0
        var applied = 0
        for (page in 0 until MAX_PULL_PAGES) {
            val response = authorized {
                client.pull(PullRequest(auth = auth(), afterCheckpoint = repository.checkpoint(), limit = PULL_LIMIT))
            }
            pulled += response.events.size
            applied += repository.applyRemotePage(response.events, response.serverCheckpoint, deviceId)
            if (!response.hasMore || response.events.isEmpty()) break
        }

        var pushed = 0
        var rejected = 0
        while (true) {
            val batch = repository.unsyncedEvents(PUSH_BATCH)
            if (batch.isEmpty()) break
            val events = batch.map {
                SyncEvent(
                    eventId = it.eventId,
                    deviceId = deviceId,
                    sequence = it.sequence,
                    entityType = it.entityType,
                    entityId = it.entityId,
                    operation = it.operation,
                    payloadJson = syncJson.parseToJsonElement(it.payloadJson).jsonObject,
                    createdAt = formatTimestamp(it.createdAt),
                )
            }
            val response = authorized {
                client.push(PushRequest(auth = auth(), baseCheckpoint = repository.checkpoint(), events = events))
            }
            val sent = batch.map { it.eventId }.toSet()
            val accepted = response.acceptedEventIds.filter { it in sent }.distinct()
            repository.markSynced(accepted)
            pushed += accepted.size
            rejected += response.rejectedEvents.size
            // Rejected events stay pending; stop rather than resend them in a loop.
            if (accepted.size < batch.size) break
        }

        return SyncResult(pushed = pushed, rejected = rejected, pulled = pulled, applied = applied)
    }
}
