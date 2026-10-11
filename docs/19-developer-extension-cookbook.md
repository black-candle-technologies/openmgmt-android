# Developer extension cookbook and change map

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Read the code in dependency order

For a first contribution, read the composition root, data entities/DAOs, repository, sync payload mapping, sync orchestration, ViewModels, shared editor/cards, then the target screen. This exposes the important boundaries before a UI change accidentally bypasses persistence or compatibility. No AGENTS.md, CONTRIBUTING.md, architecture decision record set, code formatter task, or developer policy file is tracked at this snapshot. The guidance here is derived from code and is a recommendation for maintainers, not an invented repository gate.

| Concern | Primary source |
|---|---|
| Dependency construction | `OpenMgmtApp.kt` |
| Route registration/rendering | `Destinations.kt`, `MainActivity.kt` |
| Local types and SQL | `Database.kt` |
| Mutation/event transaction | `Repository.kt` |
| Entity JSON preservation | `SyncPayloads.kt` |
| HTTP/model contract | `OmgpClient.kt`, `OmgpModels.kt` |
| Sync ordering and acceptance | `SyncManager.kt` |
| UI state and operations | `MainViewModel.kt`, `SyncViewModel.kt` |
| Task fields and saved state | `TaskEditor.kt` |
| Theme/layout behavior | `Theme.kt`, `AppScaffold.kt`, `Common.kt` |

## Example 1: expose existing task priority

Priority already exists on TaskEntity, defaults to 3, survives the editor's initial.copy, and is serialized as an integer constrained to 1–5. No database migration is needed merely to expose that existing property. Add saveable UI state keyed by the task ID and copy the chosen value into the saved object. [Task model](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L64) [Current editor state](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L194-L201) [Save copy](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L311-L323)

Illustrative code for a proposed editor change, **not present in the reviewed source and not compiled here**:

```kotlin
var priority by rememberSaveable(initial.id) {
    mutableStateOf(initial.priority.coerceIn(1, 5))
}

FieldLabel("Priority")
FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    (1..5).forEach { option ->
        FilterChip(
            selected = priority == option,
            onClick = { priority = option },
            label = { Text("P$option") },
        )
    }
}

// Include this property in the existing initial.copy(...) used by Save.
val saved = initial.copy(
    title = title.trim(),
    notes = notes.trim(),
    status = status,
    dueAt = dueAt,
    projectId = projectId,
    priority = priority,
)
```

The illustrative `coerceIn` initialization is a proposed product choice: existing outbound mapping sends any invalid value as P3, while clamping would map 99 to P5 in an opened editor. Choose a consistent invalid-value policy before applying this snippet. A P1 label should explicitly mean highest if users are expected to understand it. Decide separately whether cards and list ordering should show/use priority; changing those behaviors is not required by adding the control.

Meaningful validation is a round trip of P1 and P5, editing another field without losing priority, and restoring an open sheet after rotation. Do not claim this feature by adding only a label to TaskCard.

## Example 2: add a task field owned by Android

Suppose maintainers want estimated minutes as an editable first-class field. The current mapper preserves `estimated_minutes` only in remoteJson and inserts null when absent. A real implementation should decide null-versus-zero semantics and valid bounds first. Then update TaskEntity, bump Room version, add a non-destructive migration, change incoming/outgoing mapping so the local value becomes authoritative, extend the editor and its custom Saver, and add contract/migration tests. [Existing preserved field](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L134-L153) [Saver field order](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L109-L134) [Database schema version](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L261-L272)

Do not leave the mapper's “default if absent” rule unchanged while expecting local edits to overwrite an existing remote estimate; that would preserve the old remote value. Conversely, do not overwrite a preserved value with null simply because a newly added local property was never backfilled from remoteJson. A migration or first-read reconciliation may need to parse the old baseline. Test an existing remote estimate, a fresh task, an unknown field alongside it, an archive snapshot, and a pending-local conflict.

No migration from 3→4 is supplied here because it would be proposed implementation against a schema the reviewer could not generate or execute. Use real Room schema exports and validation when making that change.

## Example 3: add a screen

Add a `Destination` object with route/title/group, include it in ALL_DESTINATIONS, and render it in MainActivity's `when`. Reuse AppScaffold, screenPadding, and ScreenFab. Obtain task mutations through LocalTaskActions or MainViewModel rather than opening another database instance. An unknown saved route already falls back to Dashboard; consider how a renamed route affects existing saved state. [Destination registry](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/nav/Destinations.kt#L14-L46) [Render dispatch](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L92-L109)

A new task-list screen should use the same project-name and default-project identity mapping so Inbox badge suppression stays consistent. Define precisely whether it operates on the ViewModel's live filtered tasks or a new repository query that includes archives. Using a direct DAO read changes the visibility contract and needs explicit documentation. [UI engineering](18-compose-design-and-accessibility.md) describes the shared components.

## Example 4: change auth or sync environments

The injection point is OpenMgmtApp. The objects already take constructor parameters for OAuthConfig and serverUrl, but the app wires production defaults. Proposed nonproduction wiring can instantiate AuthManager with a test issuer and SyncManager with a test server. This should be part of an intentional variant/configuration design, not a runtime text box that silently sends production account tokens elsewhere. [Composition root](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/OpenMgmtApp.kt#L21-L30) [Auth constructor](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L52-L55) [Sync constructor](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L32-L37)

If an existing install changes sync servers while retaining a device ID/token/checkpoint, the stores are not namespaced by server. Decide how to separate or reset that state before testing. Likewise the cached DCR client ID is not keyed by issuer. A switch to another issuer cannot safely assume the saved bct_ ID exists there. These are consequences of the current fixed preference keys.

Changing the callback URI requires coordinated manifest/runtime predicate/config updates. The current MainActivity checks hard-coded scheme/path rather than config values, and tests assert the production defaults. Preserve public-client PKCE requirements; do not replace them with a packaged secret.

## Example 5: support a new sync entity or operation

Update the wire model only if its envelope changes; currently entity_type and operation are plain strings, so new strings can deserialize. Add a local entity/DAO/model if required, repository mutation/event production, mapper functions, replay branches, snapshot inclusion, and UI visibility as appropriate. Importantly, unsupported events are already marked processed, so a client upgraded later will not automatically recover records it skipped earlier. Design replay migration/quarantine behavior before rollout. [Supported types/operations](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L33-L44) [Remote dispatch](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L259-L289) [Processed IDs](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L244-L251)

Contract fixtures should cover entity ID mismatch, missing parent, malformed timestamp, unknown status, archived-before-local-row, repeated event, and an unrecognized future field. Do not assert desktop parity solely because class/property names look similar; test the exact serialized bytes/shapes expected by the pinned server revision.

## Example 6: background synchronization

There is currently no WorkManager dependency, service, receiver, notification implementation, or background schedule. Adding background sync is therefore an architectural change, not a new interval setting. Preserve the single-run mutex and cancellation behavior, define network/battery constraints, expose pending/error status accurately, and coordinate sign-out with in-flight work. A task queued under one account must not continue under another account after a session switch. [Current dependencies](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L52-L80) [Manifest components](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/AndroidManifest.xml#L1-L36) [Run mutex](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L60-L67)

Before adding automatic retries, resolve what to do with permanently rejected events and unsupported remote records. A periodic loop can turn a quiet pending-queue defect into repeated network traffic without repairing the data. Treat server retryable flags and HTTP status classes deliberately.

## Contributor review checklist

A useful change description states the user problem, the affected data/event contract, the behavior after the change, and actual validation performed. For data changes, include schema/migration and round-trip evidence. For UI-only changes, run the relevant interaction on representative configurations rather than inventing tests that merely repeat a constant. For auth/sync changes, use test services and redacted traces; never commit real tokens, preference files, user databases, or signing stores. Keep recommendations in this wiki separate from required project policy, since no contribution policy file is tracked.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
