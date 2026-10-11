# Migrations, backup, retained data, and recovery

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Supported database paths

The current Room version is 3. The builder registers one explicit migration, 2→3, and permits destructive migration from version 1. It does not declare a generic destructive fallback for every version or a downgrade path. The comment says v1 predates shipped builds; that is a repository claim, not independently verified deployment history. [Database version/builder](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L261-L294)

| Existing database | Configured behavior | Data implication |
|---|---|---|
| No database | Room creates current schema | Fresh local dataset |
| Version 2 | Run `MIGRATION_2_3` | Intended preservation and first-sync snapshot |
| Version 1 | Destructive migration allowed | Prior local database data may be discarded |
| Version 3 | Open current schema if valid | Normal operation |
| Other older/newer version | No explicit route shown | Do not assume upgrade/downgrade succeeds or safely resets |

No exported Room schema JSON, instrumentation migration test, or historical v2 schema fixture is tracked. Therefore this review establishes what SQL is written, not that Room validated a real v2-to-v3 device migration successfully.

## Exact 2→3 migration

| Table | Added columns | Backfill/normalization |
|---|---|---|
| tasks | createdAt INTEGER NOT NULL DEFAULT 0; startedAt INTEGER; completedAt INTEGER; remoteJson TEXT | createdAt = updatedAt; status open → inbox; priorities outside 1–5 → 3; done tasks completedAt = updatedAt |
| projects | createdAt INTEGER NOT NULL DEFAULT 0; archivedAt INTEGER; remoteJson TEXT | createdAt = updatedAt |
| organizations | createdAt INTEGER NOT NULL DEFAULT 0; archivedAt INTEGER; remoteJson TEXT | createdAt = updatedAt |
| sync_events | New table with autoincrement sequence, event identity/type/entity/operation/payload/create/sync fields | Empty initially |
| applied_remote_events | New event-ID and applied-time table | Empty initially |
| sync_state | New key/value table | snapshot_pending = "1" |

[Migration SQL](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L212-L258)

The migration does not add foreign keys, indices, account columns, or encryption. It does not immediately create Personal/Inbox or queue one event per row. Those effects happen later when `snapshotIfPending` runs during sync. Until then legacy null parents can remain visible because the ViewModel permits null project IDs. [Snapshot normalization](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L196-L219) [Null-parent visibility](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L22-L26)

## What data must be preserved

A complete preservation plan includes the Room database and its transactionally consistent journal/WAL state, not just visible task rows. It must account for all six tables: entities, pending/accepted local history, replay dedupe IDs, and checkpoint/snapshot state. `remoteJson` contains desktop-only fields that cannot be reconstructed from the simplified Android columns. Deleting it can silently drop metadata from later full-state uploads. [Database entities](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L261-L269) [JSON preservation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L24-L32)

Credentials are a separate concern: `openmgmt_auth` contains OAuth client/token/pending-flow values, while `openmgmt_sync_secure` contains device/account/user identity and device token. A previous plaintext preference file named `openmgmt_sync` is migrated lazily and cleared. Those values are not portable merely because the XML preferences can be copied. [OAuth preference keys](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L23-L29) [Secure sync preferences](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt#L15-L25) [Legacy migration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt#L61-L70)

## Platform backup configuration

The manifest sets `android:allowBackup="true"` and does not supply `fullBackupContent` or `dataExtractionRules`. The complete resources tree contains no backup-rules file. The code uses encrypted preferences backed by an AndroidX MasterKey. [Manifest](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/AndroidManifest.xml#L6-L12) [Encryption setup](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L61-L72)

Android's [EncryptedSharedPreferences reference](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) advises excluding those files from Auto Backup because restoring them can leave the encryption key unavailable. The present repository does not implement that exclusion. A restored installation can therefore encounter credential initialization failures; whether a particular device's backup transport includes or restores these files must be verified on that device. This is a backup/restore risk, not evidence that tokens are stored in plaintext.

**Recommendation:** define and test a backup policy that distinguishes user records from device-bound credentials. Handle unrecoverable credential state through a user-visible reauthentication path while preserving recoverable local records. Decide whether business records and sync bookkeeping should restore together, and validate the resulting first-sync behavior. Do not “fix” credential recovery by silently destroying the whole database.

## Recovery by symptom

| Symptom | First diagnostic | Least disruptive direction | Dangerous shortcut |
|---|---|---|---|
| Task absent from a screen | Check status, selected filter, due date, project visibility | Use All/appropriate status or restore intended parent | Reinstalling |
| Project absent | Inspect archivedAt and status on a copy | Undo if still available; otherwise planned restore | Hard-delete related rows |
| Auth fails after device restore | Look for redacted Keystore/encrypted-pref exception | Recover account/device credentials separately | Clear all app storage without preserving local data |
| Migration/open failure | Capture schema version and Room validation message | Preserve copy; test correct migration | Assume every version has destructive fallback |
| Pending events never decrease | Inspect rejected event type/operation and server contract | Fix the schema/validation mismatch | Delete pending outbox |
| Wrong-account data appears | Identify retained install dataset and snapshot history | Stop further sync and reconcile intentionally | Sign out/in again hoping for isolation |
| Corruption suspected | Preserve a consistent copy; inspect integrity read-only | Restore validated data and bookkeeping | Modify the live database while syncing |

These are recommended support sequences grounded in the application behavior; they are not an automated repair system delivered by this repository.

## Read-only support capture

Prefer a debug/test installation and developer tooling to inspect a **copy** of the database. A rooted or debuggable access path is not generally available on a production installation. Do not promise that `run-as` can read a release build. Android Studio's database inspection facilities or an authorized platform backup/export process may be appropriate when available; the app itself has no export feature.

If using adb against a debuggable build, first confirm the intended serial and app identity. Stop the process before copying a full database directory so a live WAL write is not omitted. An example diagnostic capture, executed only when authorized for that device, is:

```bash
adb shell am force-stop com.openmgmt.android
adb exec-out run-as com.openmgmt.android tar -cf - databases > openmgmt-databases.tar
```

This captures business data and event payloads; store and share it as sensitive user data. The command does not include or repair encrypted preferences and should not be presented as a complete portable application backup. Do not publish such an archive in a public issue. No capture command was run on a user's device during this review.

## Reset actions and their consequences

| Action | Business rows | Credentials | Sync behavior |
|---|---|---|---|
| Restart process | Retained | Retained | In-memory last-sync label resets |
| Sign out | Retained | OAuth tokens cleared; device identity reset; DCR ID kept | Clears checkpoint; next sync snapshots every local entity |
| Upgrade 2→3 | Intended retained/backfilled | Managed separately | Snapshot pending set |
| Upgrade from 1 | Destructive fallback permitted | Separate storage | Do not assume old entity data survives |
| Clear Android app data / uninstall | Local data at risk or removed by platform operation | Credentials lost | Unsynced local data may be unrecoverable |
| Direct outbox deletion | Entities remain | Unchanged | Some local changes lose their delivery record |

[Sign-out and sync reset](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L50-L58) [Auth removal](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L187-L198) [Migration policy](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L290-L293)

## Restoration must respect sync semantics

A database restored to an earlier point can carry old pending events, checkpoints, and applied-event IDs. A new device ID does not by itself solve entity conflicts, and resetting the checkpoint does not clear `applied_remote_events`. The snapshot path replaces pending history with current full state and protects it from incoming replay. These facts mean that a repair plan must explicitly choose the authoritative dataset before re-enabling sync; indiscriminate snapshotting can overwrite remote improvements. [Reset/snapshot](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L184-L219) [Replay dedupe/protection](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L244-L255)

There is no built-in rollback command, database version selector, archive browser, event replay console, or transactional account migration tool. Any future recovery feature should be tested against interrupted migrations, restored credentials, different accounts, retained tombstones, and rejected uploads—not just the appearance of the initial screen.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
