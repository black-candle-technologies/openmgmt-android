# Repository mutations and data-integrity contracts

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Repository responsibility

`MainRepository` is the intended write boundary for local organizations, projects, and tasks. Each local mutation uses `db.withTransaction` so the business record and its outbox event commit together. The repository exposes Room flows for live entities and the pending count, plus sync-facing methods for batching, acceptance marking, checkpoint access, snapshot reset/materialization, and remote replay. [Repository setup](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L41-L58)

The transaction invariant is narrowly defined: a local entity mutation should not become durable without the event representing it. It does not make network requests atomic with the database, does not isolate user accounts, and does not validate every model value. JSON serialization occurs inside the local transaction, so a serialization failure rolls back the surrounding mutation instead of leaving an unlogged successful write.

## `saveTask` step by step

1. Read the current clock and existing task by ID.
2. Determine whether the status is being entered: the row is new or status differs.
3. Supply a default project if the submitted project ID is null, creating default parents as needed.
4. Preserve an existing row's `createdAt`; otherwise use the submitted create time.
5. Set `updatedAt` to now.
6. Set `startedAt` to the submitted value, or now when entering in_progress.
7. Set `completedAt` to now when entering done; otherwise retain the submitted value.
8. Insert/replace the saved task.
9. Choose created, transitioned, or updated.
10. Append a full entity payload event.

[Task mutation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L62-L87)

A “transition only” event requires an existing row, a changed status, and equality of all other fields after ignoring update/start/complete timestamps and aligning status. Changing notes, title, project, priority, due date, create time, or `remoteJson` alongside status makes it updated. All three operations nevertheless carry the same full entity envelope, not a field patch. A save that changes nothing can still create an updated event and new update time; no no-op suppression is implemented.

`setTaskStatus` passes a copy to `saveTask`. It accepts a string without membership validation. The UI limits its own status choices, but a future caller cannot assume the repository enforces that list. Likewise title nonblank checks live in the editor, not this method; priority validation occurs only during outgoing JSON conversion. [Status wrapper](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L90-L90) [Editor validation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L311-L323) [Payload priority normalization](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L134-L153)

## Task archive and Undo

`deleteTask` fetches by ID and returns without an event if no row exists. Otherwise it replaces the row with canceled status and current update time, then logs an archived envelope containing only ID and archive timestamp. It does not change completed/start fields, erase notes, or delete the row. Undo through normal save can reclassify the event as transitioned if status is the only material change. [Task archive](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L92-L98) [Archive payload](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L158-L161)

Because UI Undo captures an earlier snapshot, it can overwrite later changes to the same entity if they occur before undo runs. That is a source-derived concurrency limitation, not a separate historical restore mechanism. Repository save only preserves the existing create timestamp explicitly; most fields otherwise come from the submitted object.

## Project and organization mutations

`saveProject` reads the existing row, fills a null organization ID through the default helper, preserves existing createdAt, replaces updatedAt, upserts, and emits created or updated with a full payload. `saveOrganization` performs the analogous created/update timestamp handling without parent logic. Neither validates a non-null parent, status enum, or unique name. [Project save](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L100-L113) [Organization save](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L122-L134)

`deleteProject` stores archived status plus archive/update timestamps and emits an archived event. `deleteOrganization` stores archive/update timestamps and emits an archived event. No child iteration or cascade is performed. A nonexistent ID produces no event. Repeated archive calls against retained rows can emit additional archive events. [Project archive](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L115-L120) [Organization archive](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L136-L141)

## Default-parent creation

The default helpers search all rows but explicitly require live matching records. Personal matching is case-insensitive and unarchived; Inbox matching is under the matched Personal organization, unarchived, and not archived status. New records are immediately inserted and given created events. Their creation occurs inside the caller's transaction. [Default matching](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L21-L39) [Default writes](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L146-L171)

No stable predetermined IDs are assigned, and no uniqueness check across concurrent remote datasets is made. No validation ensures that an explicitly supplied project belongs to the intended account or organization. The application does not have an account-scoped repository in this snapshot.

## Snapshot reset versus materialization

`resetSyncState` clears only `server_checkpoint` and sets `snapshot_pending` to `"1"` inside a Room transaction. It does not immediately produce events, delete business rows, clear accepted history, or clear applied-remote IDs. The credential reset is a separate operation performed by `SyncManager.disconnect`. [Reset](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L182-L188) [Disconnect ordering](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L50-L58)

`snapshotIfPending` runs before pulling. When the flag exists it:

1. Deletes all pending outbox rows.
2. Files projects with null organization IDs under Personal.
3. Files tasks with null project IDs under Inbox.
4. Deletes pending rows again to remove helper-created events superseded by the snapshot.
5. Emits one **updated** full-entity event per organization, then project, then task, including archived/canceled rows.
6. Clears the snapshot flag.

[Snapshot implementation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L190-L219)

The snapshot collapses edit history to current full state. It repairs null parents only, not missing non-null parents. Archive state is carried in the full entity's fields rather than separate archive events. The early snapshot creates pending protection so subsequent remote replay cannot overwrite the snapshot's local rows before they are pushed. This design also explains why, after sign-out and a different account's sign-in, a later sync attempts to transfer the retained local dataset under that account's newly registered device credentials. Remote transfer still depends on successful requests and server acceptance.

## Remote page application

Each page is processed in one transaction. It is stably sorted by entity category: organizations first, projects second, everything else third. Within a type, original list ordering is retained. Events from the current device are ignored as echoes; event IDs already present in `applied_remote_events` are ignored as duplicates. For a new remote event, pending local events on the same `(entityType, entityId)` suppress the handler. Regardless of suppression or a caught handler exception, the event ID is then marked applied. A nonempty page checkpoint is saved at the end. [Page replay](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L231-L257)

A malformed/unsupported individual event does not roll back the page because `applyRemote` is wrapped in `runCatching`; its exception is logged and processing continues. An uncaught error in other transaction work can roll back the page. There is no failed-event queue or user-visible skipped-event count. “Applied” in the deduplication table should therefore be understood as processed, not proof of successful entity mutation.

## Remote operation validation

| Case | Validation and outcome |
|---|---|
| archived | Payload `id` must equal envelope entity ID; archive timestamp must parse |
| archived organization | Update existing organization's archive/update times; missing row is a no-op |
| archived project | Update status and archive/update times; missing row is a no-op |
| archived task | Update canceled status and update time; missing row is a no-op |
| created/updated/transitioned | Require one of these operation strings; require payload entity object; require inner ID equality |
| supported entity type | Map into one of three Room entity classes and insert/replace |
| unsupported type/operation or malformed data | Throw from handler; replay catches, logs, then marks applied |

[Remote handler](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L259-L289)

No local outbox event is generated during remote replay. This avoids echo loops. However, client-side replay does not authenticate event signatures, enforce account/target/workspace routing, or compare update timestamps. The server is expected to return authorized events in its intended order, and the client trusts that boundary. [Protocol](14-omgp-protocol-reference.md) and [security](17-security-and-hardening.md) describe that contract.

## Developer invariants

When adding an operation, preserve entity/event atomicity, deterministic event identity, same-entity pending protection semantics, checkpoint/page atomicity, and separation of replay from local write logging. Decide explicitly whether a new field belongs in the local model, preserved JSON, or both. If a UI action changes a field not included in the outgoing representation, its local success will not establish cross-device correctness.

For repair tooling, a direct SQL update is not equivalent to calling the repository. It can bypass timestamps, required parent creation, payload preservation, and outbox generation. An authorized repair must plan those side effects together and be tested against a copy before it touches the user's authoritative data. These are implementation-derived constraints, not an additional repository policy file.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
