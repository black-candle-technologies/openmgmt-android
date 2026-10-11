# Task user guide and exact state transitions

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Create and edit a task

Use New task from Dashboard, Tasks, Schedule, or Board, or Add for today from Daily Operations. The shared bottom sheet exposes Title, Notes, Status, Due, and Project. Save requires a nonblank title, trims title and notes, preserves fields the editor does not expose, and delegates persistence to the repository. Closing or canceling the sheet does not save a draft to Room. [Task editor](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L194-L324)

| Entry point | Draft status | Draft due date | Draft project |
|---|---|---|---|
| Dashboard | inbox | none | null → default Inbox when saved |
| Tasks, All selected | inbox | none | default Inbox |
| Tasks, status filter selected | selected status | none | default Inbox |
| Daily Operations | inbox | today's local start | default Inbox |
| Schedule | inbox | none | default Inbox |
| Board, To do | inbox | none | default Inbox |
| Board, In progress | in_progress | none | default Inbox |
| Board, Blocked | blocked | none | default Inbox |
| Board, Done | done | none | default Inbox |

The defaults are defined at each screen's action, not inferred from a common “today” or “active project” context. [Task-list action](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskListScreen.kt#L57-L59) [Daily action](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L47-L49) [Board action](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt#L28-L49) [Schedule action](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L45-L45)

### Title and notes

The title field permits multiple displayed lines and the notes field displays between two and six lines before its text area handling applies. There is no explicit maximum string length, server-validation preview, Markdown renderer, attachment picker, or checklist structure in the editor. Notes are an ordinary string in Room and become `description` in outgoing entity JSON; blank notes serialize as JSON null. Trimming means leading/trailing whitespace in these fields is not preserved through an Android save. [Editor text fields](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L225-L240) [Save copy](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L311-L323) [Notes mapping](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L134-L141)

### Status

| Stored value | UI label | Visible board group | Meaning enforced by code |
|---|---|---|---|
| `inbox` | Inbox | To do | Default task state |
| `backlog` | Backlog | To do | Selectable state; no additional workflow validation |
| `scheduled` | Scheduled | To do | State alone does not create a date |
| `ready` | Ready | To do | Destination when reopening by checkbox |
| `in_progress` | In progress | In progress | Entering can set `startedAt` |
| `blocked` | Blocked | Blocked | Caution styling; no required reason |
| `waiting` | Waiting | Blocked | Caution styling; no dependency model |
| `done` | Done | Done | Entering sets `completedAt` |
| `canceled` | Not selectable | Hidden | Delete/archive representation |

No finite-state transition matrix restricts moves between selectable statuses. The editor can go directly from inbox to done or from blocked to in_progress. A task's status is a string in the database, so a remote unknown status can exist without matching a selectable chip; the Board will omit it because it belongs to no group. [Status constants](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L18-L34) [Repository save](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L62-L87) [Board groups](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt#L28-L37)

### Due date

Today and Tomorrow set the chosen local calendar day's start, Pick date converts the date-picker's UTC day representation into local midnight, and Clear sets `dueAt` to null. There is no time-of-day picker. Editing another field preserves an existing due timestamp; choosing a date deliberately replaces it with local midnight, which can discard a desktop-set time of day. [Due controls](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L257-L289) [Date-picker conversion](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L328-L356)

### Project

The project dropdown lists live projects and is disabled when there are none. With no selected/resolvable project, it displays “Inbox.” On save, only a null ID causes the repository to choose/create the default project. A stale non-null ID is not automatically replaced merely because its name cannot be resolved. Projects can be created on the Projects screen or Dashboard; there is no inline project-creation action in the task editor. [Project picker](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L372-L407) [Null-parent fallback](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L62-L73)

## Complete, reopen, and undo

Checking any nondone task writes done and displays a short Undo snackbar. Undo saves the original task snapshot. Unchecking a done task changes it to ready without an undo snackbar. It does not restore the task's prior status. Editing the status field is the route to another state. [Shared actions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L147-L161)

Repository timestamp rules matter to downstream integrations:

- A new task counts as entering its initial status.
- `createdAt` is retained from the existing database row on later saves.
- Every save sets `updatedAt` to the current clock.
- Entering in_progress sets `startedAt` only if the submitted task has no start timestamp.
- Entering done sets `completedAt` to the current clock.
- Leaving done does not automatically clear `completedAt`; leaving in_progress does not clear `startedAt`.

These are the actual persisted rules. Consumers should use current status, not the mere existence of a completion timestamp, to decide whether the Android task is done. [Timestamp logic](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L62-L78)

Undo uses the task captured before the action, not a three-way merge with later changes. It records another normal mutation, with a new update time and sync event. The snackbar helper dismisses a currently visible snackbar before showing the new one, so rapid operations do not create a durable queue of undo opportunities. Use Undo promptly and avoid treating it as edit history. [Undo helper](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L91-L102)

## Delete is an archive action

Delete is available only when editing an existing task. It changes the task's status to canceled, updates the timestamp, and logs an `archived` event containing ID and archive time. It does not physically delete the `tasks` row or remove historic sync events. All task observers exclude canceled rows, and there is no archive browser in the Android UI. The immediate Undo resaves the original task snapshot and makes it visible again if its parent is live. [Delete UI](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L169-L179) [Delete persistence](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L92-L98) [DAO visibility](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L119-L132)

Deleting a task differs from deleting its project. Project archiving hides children via the ViewModel's parent filter without modifying each child status; restoring the project can reveal all those children together. [Structure and archiving](06-structure-and-archiving.md) explains that behavior.

## Priority and desktop-only fields

Tasks store an integer priority with default 3, and outbound mapping clamps a value outside 1–5 back to 3. The Android editor does not provide a priority control, cards do not show it, and the inspected screen sort orders do not use it. A synced priority can remain stored and be sent later without being visible to the user. The other modeled hidden fields are IDs, update/create/start/complete timestamps, and `remoteJson`. [Task entity](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L64) [Priority normalization](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L134-L153)

Unknown desktop fields are preserved in accepted remote JSON and merged into subsequent outgoing entities. That permits many round trips without Android discarding fields it cannot edit, but it is not a guarantee under concurrent edits: a remote event skipped because local edits are pending never updates that JSON baseline. [Entity mapping](16-entity-json-interoperability.md) documents exact defaults and exceptions.

## Recognize normal list changes

Tasks can disappear from a current view because they became done, changed status, received another due date, were canceled, or belong to an archived/missing project. Tasks remains the broadest local view, but even All excludes canceled work and children of non-live project IDs. Dashboard only shows six attention items, Daily Operations only shows five upcoming items, and Board has explicit status groups. A missing card is not by itself proof of failed persistence or synchronization. [ViewModel visibility](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L22-L26) [Dashboard cap](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt#L72-L76) [Daily cap](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L43-L45)

## Draft persistence and errors

The editor uses saved state for both its open task snapshot and edited fields to survive activity recreation such as rotation. This is distinct from a committed database save and is not an exportable draft system. The editor closes before the ViewModel's launched repository mutation is known to have completed, and those mutation methods do not expose a per-save success/error result. If a save appears not to stick, verify the filter/parent state first, then collect a redacted crash/log report rather than assuming a success snackbar certified persistence. [Editor saver](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L104-L145) [Close then save](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L204-L209) [Mutation methods](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L37-L47)

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
