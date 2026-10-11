# Installation and first run

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Choose an identifiable build

This repository's workflows produce a debug APK artifact named `openmgmt-debug-apk` at `app/build/outputs/apk/debug/app-debug.apk`. They do not contain a store-publishing job, a GitHub Release creation step, a release signing configuration, or an end-user update service. Obtain an APK from a trusted build with a recorded source commit and signer, or build it yourself using [the setup guide](02-build-and-development-setup.md). Do not infer that a current store listing or release download was verified by this source review. [Debug artifact](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/build.yml#L36-L45) [Release task](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L128-L152)

The application ID is `com.openmgmt.android`, and the declared minimum API is 26. The version displayed in Settings is derived from `BuildConfig`, so a build of this snapshot should report `0.3.3 (7)`. A device may contain another revision; check its About section before comparing symptoms with this wiki. [Identity](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L9-L19) [Version display](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SettingsScreen.kt#L70-L74)

## Install on a development device

With adb configured and the user's chosen device connected:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.openmgmt.android/.MainActivity
```

Confirm the target device before using any install, stop, data-export, or removal command. An existing install must have a compatible application ID and signing certificate to be updated in place. If installation reports a signature mismatch, stop and decide how to preserve the existing local data; uninstalling to make the error disappear is a data-loss operation. This app has no built-in export UI. [Recovery guidance](12-migrations-backup-and-recovery.md) explains what a backup must preserve.

For installation through Android's file installer, use the device's normal explicit approval for the trusted APK source. Do not broadly disable device protections. The source declares only `INTERNET`; it does not request contacts, storage, microphone, camera, notification, or location permissions. There is no first-run permission wizard in the inspected app. [Manifest](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/AndroidManifest.xml#L1-L36)

## What first launch does

`OpenMgmtApp.onCreate` obtains the Room singleton, constructs `MainRepository`, creates `AuthManager`, and creates `SyncManager` with encrypted sync credentials. `MainActivity` obtains two ViewModels and displays the Compose application. The initial route is Dashboard; there is no blocking login screen or onboarding sequence. State observers initially contain empty lists while data is loaded. [Startup composition](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/OpenMgmtApp.kt#L21-L30) [Initial UI route](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L60-L78) [Initial Flow state](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L19-L35)

Opening the app does not seed `Personal` and `Inbox` immediately. Those defaults are created when a save needs them. Therefore a genuinely empty installation can initially show no projects or organizations, and then show both after the first task is saved. Local task mutations are usable before any sign-in. [Lazy default creation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L62-L74) [Default helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L146-L161)

## First local task walkthrough

1. On Dashboard, choose **New task**.
2. Enter a nonblank title. Add notes if useful.
3. Leave **Inbox** selected or choose another status.
4. Optionally choose **Today**, **Tomorrow**, or **Pick date**.
5. Leave the project at its displayed Inbox default on a fresh install.
6. Choose **Add task**. The sheet closes and the repository writes the task and event transactionally.
7. Open Tasks from the navigation drawer to see the complete task list.

The editor trims title and notes on Save. It does not expose a priority control; a new task uses P3 in the entity model. Choosing a due date does not automatically change its status to `scheduled`. [Editor controls/save](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L225-L324) [Default task values](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L64) [Save transaction](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L62-L87)

If this is the first unassigned task, the new `Personal` organization and `Inbox` project are real records with generated IDs. In the Sync screen, “Changes to send” can therefore show three events for a single task. This is expected and does not mean the task was duplicated.

## First synchronization walkthrough

Open **Sync**, choose **Sign in**, and finish the Black Candle account flow in the system browser. The app dynamically registers a public OAuth client if this installation has none, creates PKCE/state values, and waits up to five minutes for the callback. On successful return, choose **Sync now**. This performs server compatibility negotiation, initial device registration, pull/replay, and local event push. Sign-in itself does not trigger the sync run. [Sign-in lifecycle](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L79-L115) [User-facing sign-in result](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L52-L68) [Sync order](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L108-L155)

The callback URI is `com.openmgmt.android:/oauth2/callback`: there is one slash after the colon and no hostname. This spelling is part of the client/server contract. The app does not ask you to copy a client secret, paste a device token, or edit an OAuth metadata URL. [OAuth config](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/OAuthConfig.kt#L12-L17) [Dynamic client registration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L203-L227)

After a successful sync, check both the message and **Changes to send**. A returned result can contain rejected changes; those stay pending. The top status pill is not a durable proof that every current edit is uploaded. [The sync guide](08-account-and-sync-user-guide.md) covers the status combinations.

## First-run acceptance checklist

For a maintainer accepting a fresh build, verify these behaviors with a test account and disposable dataset:

| Action | Expected implementation behavior |
|---|---|
| Launch without an account | Dashboard opens; local creation is available |
| Create an unassigned task | Personal/Inbox are created or reused; task is visible |
| Rotate with an editor open | Editor task snapshot and draft fields are intended to survive recreation |
| Complete a task | Status becomes done; Undo appears |
| Reopen a done task | Status becomes ready |
| Set a due date | Date appears on cards; schedule placement follows local date semantics |
| Sign in | Browser callback saves account tokens; registration waits for Sync now |
| Sync | Device registration, pull, and push occur; counters/status update |
| Restart the process | Local records and credentials remain; “Last sync” returns to “Not this session” |

These are source-derived acceptance expectations, not tests executed during this review. [Editor persistence](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L104-L145) [Completion semantics](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L151-L160) [Session state](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L22-L40)

## Device sharing and switching accounts

Treat the installation as a continuing local workspace. **Sign out keeps all local records and marks a full snapshot pending.** After another account signs in, a later sync materializes that snapshot and attempts to push it under the newly registered account's device credentials; the server must accept the events for transfer to complete. It also retains the dynamic OAuth client ID. There is no account-specific database switch, user profile picker, or local erasure control. Do not hand the phone to a different user on the assumption that signing out separates their data. Resolve data retention and verified recovery before any deliberate reset. [Sign-out](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L187-L198) [Disconnect](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L50-L58) [Snapshot](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L196-L219)

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
