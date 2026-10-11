# Complete Room schema and DAO reference

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Database identity and modeling rules

`AppDatabase` is Room schema version **3**, named **`openmgmt.db`**, with six entity tables. `exportSchema` is false, and no generated schema JSON is tracked. The tables below describe Kotlin properties and their logical SQLite storage classes; Kotlin constructor defaults are **not** declarations of SQL defaults for arbitrary direct SQL inserts. The authoritative generated Room schema must be obtained from an actual build when developing a migration. [Database declaration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L261-L277) [Builder](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L283-L294)

All business IDs are strings generated from `UUID.randomUUID()` by default. Relationships are nullable strings, with no foreign keys or secondary indices declared. No business table has an account ID, user ID, or workspace ID column. Consequently the database is one installation-wide dataset. Credentials are stored elsewhere. [Business entities](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L87)

## Exact entity-class to table mapping

All entity classes below are in package `com.openmgmt.android.data` and are declared in `Database.kt`. `AppDatabase` registers all six; `MainRepository` uses the first three as its business records.

| Kotlin class | Room table | Primary key |
|---|---|---|
| `TaskEntity` | `tasks` | `id` |
| `ProjectEntity` | `projects` | `id` |
| `OrganizationEntity` | `organizations` | `id` |
| `SyncEventEntity` | `sync_events` | Auto-generated `sequence` |
| `AppliedRemoteEventEntity` | `applied_remote_events` | `eventId` |
| `SyncStateEntity` | `sync_state` | `key` |

[Entity declarations](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L117)

## `tasks`

| Property/column | Kotlin / SQLite class | Null? | Constructor default / purpose |
|---|---|---|---|
| `id` | String / TEXT | No | UUID; primary key |
| `title` | String / TEXT | No | Required constructor argument |
| `notes` | String / TEXT | No | Empty string; maps to remote description |
| `status` | String / TEXT | No | `inbox` |
| `priority` | Int / INTEGER | No | 3; intended range 1–5 |
| `dueAt` | Long / INTEGER | Yes | null; due instant in epoch milliseconds |
| `projectId` | String / TEXT | Yes | null until save fallback or received remote data |
| `updatedAt` | Long / INTEGER | No | Current clock |
| `createdAt` | Long / INTEGER | No | Current clock |
| `startedAt` | Long / INTEGER | Yes | null |
| `completedAt` | Long / INTEGER | Yes | null |
| `remoteJson` | String / TEXT | Yes | null; full last accepted remote entity JSON |

Status is not a Room enum or SQL CHECK constraint. Priority is not constrained by Room either. The repository supplies a default project for null IDs; outbound serialization normalizes invalid priority to 3, while inbound mapping accepts any parseable integer. A non-null invalid parent can remain stored. [Task entity](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L64) [Priority mapping](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L134-L153) [Inbound task](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L183-L196)

## `projects`

| Property/column | Kotlin / SQLite class | Null? | Constructor default / purpose |
|---|---|---|---|
| `id` | String / TEXT | No | UUID; primary key |
| `name` | String / TEXT | No | Required argument |
| `organizationId` | String / TEXT | Yes | null until save fallback or remote import |
| `status` | String / TEXT | No | `active` |
| `updatedAt` | Long / INTEGER | No | Current clock |
| `createdAt` | Long / INTEGER | No | Current clock |
| `archivedAt` | Long / INTEGER | Yes | null |
| `remoteJson` | String / TEXT | Yes | Full remote entity baseline |

Visibility requires both no archive timestamp and status other than archived. A paused or completed project remains visible. Names/slugs are not local unique constraints; slug exists only inside the full remote JSON, not as a dedicated column. [Project model and query](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L66-L77) [Project DAO](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L134-L147)

## `organizations`

| Property/column | Kotlin / SQLite class | Null? | Constructor default / purpose |
|---|---|---|---|
| `id` | String / TEXT | No | UUID; primary key |
| `name` | String / TEXT | No | Required argument |
| `updatedAt` | Long / INTEGER | No | Current clock |
| `createdAt` | Long / INTEGER | No | Current clock |
| `archivedAt` | Long / INTEGER | Yes | null |
| `remoteJson` | String / TEXT | Yes | Full remote entity baseline |

Organizations have no separate status column. Archive visibility is solely `archivedAt IS NULL`. Description, color, icon, and slug can be retained inside `remoteJson` even though the local model exposes only name and timestamps. [Organization model](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L79-L87) [Outbound organization mapping](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L98-L111)

## `sync_events` — durable local outbox

| Property/column | Kotlin / SQLite class | Null? | Constructor default / purpose |
|---|---|---|---|
| `sequence` | Long / INTEGER | No | 0 on construction; auto-generated primary key |
| `eventId` | String / TEXT | No | UUID; network event identity |
| `entityType` | String / TEXT | No | Required: organization/project/task for current callers |
| `entityId` | String / TEXT | No | Required affected ID |
| `operation` | String / TEXT | No | created/updated/transitioned/archived for current callers |
| `payloadJson` | String / TEXT | No | Serialized envelope object |
| `createdAt` | Long / INTEGER | No | Current clock; event timestamp |
| `syncedAt` | Long / INTEGER | Yes | null while pending; current clock when accepted |

The migration explicitly creates `sequence INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL`. Event UUID is not declared unique or indexed. The device ID is not stored in the local event; the current device ID is stamped when the manager constructs the outgoing event. Accepted event rows are retained; no purge operation removes them. Reset/snapshot can delete pending rows, not accepted history. [Outbox entity](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L89-L104) [Outbox DDL](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L239-L245) [Push mapping](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L128-L148)

An event payload can contain the same task notes and remote-only data as the business record. Encrypting token preferences does not encrypt these duplicated business payloads. Storage/retention policy must cover both entity tables and the outbox.

## `applied_remote_events` — replay deduplication

| Column | Kotlin / SQLite class | Null? | Purpose |
|---|---|---|---|
| `eventId` | String / TEXT | No | Primary key of a processed remote event |
| `appliedAt` | Long / INTEGER | No | Current local time when marked |

“Applied” includes remote events skipped for pending-local conflicts and events whose handler threw and was logged. Echoes from the current device and already recorded IDs are skipped before insertion. There is no payload, error, account ID, replay status, retention policy, or delete-all DAO operation in this table. Resetting sync state does not clear it. [Applied entity](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L106-L111) [Replay logic](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L244-L255)

## `sync_state` — durable small state

| Column | Kotlin / SQLite class | Null? | Purpose |
|---|---|---|---|
| `key` | String / TEXT | No | Primary key |
| `value` | String / TEXT | No | Opaque string |

Defined keys are `server_checkpoint`, the last nonempty checkpoint from a fully processed pull page, and `snapshot_pending`, conventionally set to `"1"` when a full local re-push is needed. Snapshot detection tests whether the key exists, not whether its value equals `"1"`. Checkpoints are opaque and not parsed or numerically compared. [State definition/keys](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L113-L117) [Keys](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L203-L210) [Snapshot presence test](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L196-L198)

## Business DAO method catalog

| DAO method | Exact semantics |
|---|---|
| `TaskDao.observeAll()` | Flow of tasks where status != canceled, ordered updatedAt descending |
| `TaskDao.getAll()` | All tasks including canceled; no ORDER BY |
| `TaskDao.get(id)` | Nullable row by primary ID |
| `TaskDao.upsert(task)` | `@Insert(REPLACE)` |
| `ProjectDao.observeAll()` | Flow where archivedAt null and status != archived, ordered name ascending |
| `ProjectDao.getAll()` | All project rows including archives; no ORDER BY |
| `ProjectDao.get(id)` | Nullable row by ID |
| `ProjectDao.upsert(project)` | `@Insert(REPLACE)` |
| `OrganizationDao.observeAll()` | Flow where archivedAt null, ordered name ascending |
| `OrganizationDao.getAll()` | All organizations; no ORDER BY |
| `OrganizationDao.get(id)` | Nullable row by ID |
| `OrganizationDao.upsert(org)` | `@Insert(REPLACE)` |

[All business DAOs](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L119-L162)

The methods named upsert use insert-with-replace semantics rather than Room's `@Upsert`. There are no DAO hard-delete methods for business rows. Parent filtering is not embedded in these SQL queries: `MainViewModel` adds the live-project task condition, while organization visibility is not joined into project visibility. Direct DAO consumers will therefore see a different set from UI consumers. [ViewModel projection](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L22-L26)

## Sync DAO method catalog

| Method | Query/write behavior | Caller consequence |
|---|---|---|
| `insertEvent(event)` | Ordinary insert; returns generated Long | Fails on primary-key conflicts; no replace behavior |
| `unsyncedEvents(limit)` | Null syncedAt, sequence ascending, LIMIT parameter | Oldest pending events form each batch |
| `observeUnsyncedCount()` | COUNT of null syncedAt rows | Counts events, not unique entities |
| `markSynced(ids, time)` | UPDATE by eventId IN list | Marks every matching row; no sequence check |
| `deleteUnsynced()` | DELETE rows with null syncedAt | Snapshot replaces pending history |
| `hasPending(type, id)` | EXISTS of pending event for entity pair | Shields an entire entity from replay |
| `isApplied(eventId)` | EXISTS in dedupe table | Global event-ID dedupe within this database |
| `markApplied(row)` | INSERT IGNORE | Repeated IDs do not fail |
| `getState(key)` | Nullable value | Absent checkpoint/snapshot signal |
| `setState(row)` | INSERT REPLACE | Overwrites same key |
| `clearState(key)` | DELETE by key | Used for checkpoint and snapshot transitions |

[Sync DAO](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L164-L201)

## Inspection guidance

Use read-only inspection against a consistent copy, not the live app's potentially active database. Useful diagnostic queries include:

```sql
PRAGMA user_version;
SELECT status, COUNT(*) FROM tasks GROUP BY status;
SELECT COUNT(*) AS pending_events FROM sync_events WHERE syncedAt IS NULL;
SELECT entityType, operation, COUNT(*)
FROM sync_events WHERE syncedAt IS NULL GROUP BY entityType, operation;
SELECT key, value FROM sync_state;
SELECT COUNT(*) AS tasks_with_missing_project
FROM tasks t LEFT JOIN projects p ON p.id = t.projectId
WHERE t.projectId IS NOT NULL AND p.id IS NULL;
SELECT COUNT(*) AS projects_with_missing_organization
FROM projects p LEFT JOIN organizations o ON o.id = p.organizationId
WHERE p.organizationId IS NOT NULL AND o.id IS NULL;
```

These queries reveal state without altering it. A missing parent, archived parent, and canceled task require different repairs. Do not clear the outbox or edit archive values as a first response to a sync complaint: direct SQL does not emit a synchronization event. [Recovery](12-migrations-backup-and-recovery.md) describes preservation, and [repository semantics](11-repository-and-data-integrity.md) describes safe mutation boundaries.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
