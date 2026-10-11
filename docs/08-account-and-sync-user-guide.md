# Account sign-in and synchronization user guide

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Two credentials, two UI states

Black Candle sign-in and sync registration are separate. Sign-in stores an OAuth access token, possible refresh token, and expiry. Device registration sends that access token to the sync service and receives a device token, account ID, and user ID. Subsequent push/pull operations use the device token. The UI computes `canSync` as signedIn OR registered; it does not require both to be true. [Account/device state](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L20-L32) [Registration and context](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L73-L96)

| Account state | Device state | What the UI permits | Interpretation |
|---|---|---|---|
| Not signed in | Not registered | Sign in | Local work remains available; no sync credential |
| Signed in | Not registered | Sync now, Sign out | Next sync attempts registration |
| Signed in | Registered | Sync now, Sign out | Ordinary connected state |
| Not signed in | Registered | Sync now, Sign in | Device credential can operate independently of OAuth |

“Signed in” tests whether the access-token preference exists. It does not test expiry, fetch your profile, validate an ID token, or display a name/email. “Registered” tests whether a device-token preference is non-null. Neither indicator makes an online request solely to establish its status. [Auth presence check](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L74-L77) [Registration check](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt#L28-L36)

## Sign in

Choose **Sync → Sign in**. On first use the client posts public-client metadata to the issuer's `/oauth/register` endpoint and requires a nonempty `bct_` client ID. It then launches the system browser through a Custom Tab for authorization. The user authenticates with the issuer in that browser; the app does not collect the password in a native field. The expected redirect is `com.openmgmt.android:/oauth2/callback`. [Registration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L203-L229) [Authorization launch](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L79-L115)

The auth service reviewed alongside this app returns a browser page for native callbacks after approval. It attempts to reopen the app automatically and provides a **Return to app** link; tap that link if the browser remains on the return page. This behavior comes from the separately pinned auth source and was not exercised on a device during this review. [Auth native return choice](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth.go#L390-L411) [Return page and link](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth.go#L881-L906)

PKCE verifier and random state are saved before the browser opens, allowing a callback after the app process was killed. The active waiting operation times out after five minutes. Returning successfully stores tokens and displays “Signed in. Your next sync will register this device.” Sign-in does not itself start sync. The registration part of the message applies when device registration is absent: an existing device credential is reused until a protocol unauthorized response triggers re-registration. [Persisted pending flow](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L82-L114) [Sign-in UI result](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L52-L68)

If the browser is dismissed, no browser-dismiss callback cancels the sign-in immediately; the waiting state can continue until timeout. If the app restarts while the browser is active, the persisted verifier/state may still complete authentication, but the in-memory waiter and message history are gone. Cold-start failures are logged by `MainActivity`; a visible “Not signed in” state alone may not explain the cause. [Redirect forwarding](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L117-L138)

## Run a sync

Use Sync now on the Sync page or the top-bar refresh icon. The UI prevents overlapping sign-in/sync activity, and the sync manager separately rejects another simultaneous sync. A run does the following:

1. Sends a protocol hello containing app version and device ID.
2. Registers the device when it has no device token.
3. Materializes a full snapshot if reset/migration marked one pending.
4. Pulls remote event pages and replays them locally.
5. Pushes pending local events in batches.
6. Updates the in-memory last-sync time and result message.

The app does not schedule automatic runs, synchronize on every edit, or maintain a live connection. Leaving the Sync screen does not cancel the ViewModel's coroutine, but process termination ends the current run. Durable local events/checkpoints allow a later manually initiated run to continue according to the sync algorithm. [Run order](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L60-L155) [ViewModel activity ownership](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L13-L17)

## Read the Sync page correctly

| Field/message | Meaning | What it does not prove |
|---|---|---|
| Black Candle account | Access-token preference presence | Token validity, account identity, or server reachability |
| This device | Device-token preference presence | Successful registration against the server right now |
| Last sync | Latest successful `syncOnce` return this ViewModel lifetime | Persistent sync history or an error-free entire dataset |
| Changes to send | Count of `sync_events` with null `syncedAt` | Distinct tasks, bytes, or a server backlog |
| Sent N | Number of returned accepted IDs belonging to the pushed batch | Complete server replay by another device |
| Received N | Number of remote apply calls counted successful | Total remote events downloaded or actual changed rows |
| Rejected changes | Server returned per-event rejections | Detailed causes; UI only shows the number |

The result contains both `pulled` and `applied`, but the UI prints `applied` as received. Echoes, duplicate IDs, pending-local conflicts, and malformed/unsupported events reduce this count; an archive of an already absent entity can count as applied without changing a row. [UI result](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L71-L92) [Replay count](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L231-L289)

The last-sync value is lost when the ViewModel is recreated after process death. “Not this session” after reopening is expected even if credentials and database data persist. A result with rejected events is still a successful function return for timestamp purposes, but `isError` becomes true. [State defaults](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L22-L29) [Success/error calculation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L78-L91)

## Top-bar status pill

Precedence is Syncing, Signing in, Error, Local only, Up to date, then Not synced. Up to date requires only a non-null current-session last-sync timestamp after higher-priority states are ruled out. It does not compare the database event count and does not reset when a later local edit creates a new event. Always check Changes to send if upload completeness matters. [Pill rules](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L276-L288)

## What happens to conflicts

When a pulled event targets an entity with unsent local events, Android keeps the local row and marks that remote event applied. The later push sends the full local entity snapshot. This deliberately favors this client's pending edits, and it can replace concurrent remote changes to other fields. There is no conflict dialog or field-level merge. To reduce accidental overwrites, manually sync before editing on another device and after completing a batch of work. That practice reduces overlap; it is not an atomic multi-device lock. [Pending protection](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L221-L255)

Rejected local events remain pending. The push loop stops when fewer than the whole batch were accepted. Do not repeatedly clear app data or sign out just to force a green label: those actions have distinct data and account consequences. A rejected event needs protocol-level diagnosis, available in [the troubleshooting runbooks](22-troubleshooting-runbooks.md).

## Sign out and account switching

Sign out removes OAuth access/refresh tokens, expiry, and pending flow values. It retains the public dynamic client ID. The sync disconnect generates a new device ID, removes the device token/account/user IDs, clears the pull checkpoint, and marks a complete local snapshot pending. It leaves all business data and historic sync rows in Room. [OAuth sign-out](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L187-L198) [Device reset](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt#L46-L59) [Sync reset](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L184-L219)

**A later sync after another account signs in attempts to send the retained installation dataset, including archived rows, under the newly registered account's device credentials.** Sign-out sets the pending-snapshot marker; sign-in alone sends nothing. Sync then materializes the snapshot before pull and attempts push. Remote transfer depends on successful requests and server acceptance; even a completed run can leave rejected events pending. This client behavior does not provide account isolation. There is no confirmation that names the outgoing and incoming account, and the app does not display account identity. This makes account switching a data-transfer decision rather than a harmless profile toggle.

If a device credential remains registered while account-token state is absent, signing in again does not automatically replace that registration. The sync manager initially registers only an unregistered device; otherwise it uses the saved device credential and account/user hints, with one re-registration attempt only after a protocol unauthorized response. A sign-in success message is therefore not proof of a device/account rebind. [Registration condition](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L91-L110)

Sign out makes no remote revocation request and does not log the browser out of the issuer. Previously issued tokens and the server's old device record are not proven revoked merely because local preferences were cleared. If server-side revocation is needed, use the auth/sync service's separately documented controls. When only a device token remains and no access token is present, these screens do not expose Sign out; support for that state requires an intentional disconnect UX improvement. [Sign-out visibility](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SyncScreen.kt#L94-L123) [Settings sign-out visibility](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SettingsScreen.kt#L36-L57)

## Connection problems and safe next steps

For connection failures, record the version, operation, visible error text, time, pending event count, and whether the device is registered. Avoid sharing callback URLs or preferences: callback codes and all tokens are credentials. No raw HTTP response body or structured rejected-event detail is exposed in the UI, so a maintainer may need a redacted trace from a test build. A fresh sign-in is appropriate after a genuinely expired session, but it will not repair malformed entity payloads, unsupported protocol events, or database visibility filters. See [OAuth internals](13-oauth-and-credentials.md) and [troubleshooting](22-troubleshooting-runbooks.md).

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
