# Projects, organizations, defaults, and archiving

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Logical hierarchy and identity

Organizations contain projects and projects contain tasks through ID fields. All three entity types generate UUID strings by default. The database does not declare foreign keys, cascade rules, unique name constraints, or a stored account/workspace column on these entities. Thus hierarchy is maintained by application conventions, and organization names are labels rather than access-control boundaries. [Entity definitions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L87)

`Personal` and `Inbox` are fallback records, not reserved database singleton IDs. `defaultOrganizationOf` finds the first unarchived organization whose name equals Personal ignoring case. `defaultProjectOf` finds the first unarchived, nonarchived-status Inbox project under that organization. A custom organization named Personal can be reused. Renaming the fallback or archiving it changes future fallback matching; later unassigned saves can create another Personal or Inbox with a new UUID. [Default matching](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L21-L39) [Creation helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L146-L161)

There is no startup seed that ensures these records always exist. The first project without an organization creates Personal if needed. The first task without a project creates Personal and Inbox if needed. Both parent creation and the final child save run inside the outer repository transaction and record sync events in parent-before-child creation order.

## Project workflow

Choose New project on Projects or the Project quick action on Dashboard. Enter a nonblank name and Create. The shared name dialog trims input. Creation does not ask for an organization or project status; the model starts active and the repository supplies Personal. To reorganize it, tap its card and choose an organization in the editor. [Project creation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L119-L126) [Input validation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L303-L342) [Project defaults](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L66-L77)

The editor supports name, Active/Paused/Completed, organization, Save, Cancel, and Delete. Paused and completed projects remain visible, and their tasks remain visible. Only `archivedAt != null` or status archived excludes a project from the project observer. A completed project does not complete its tasks; no cascade mutation is performed. [Editor](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L257-L336) [Project query](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L134-L146)

### Project cards and ordering

Active projects are shown first, followed by all other visible statuses. The underlying DAO orders by name ascending, and the status grouping preserves that input order. Each card displays the count of nondone visible tasks, an organization name if its ID resolves among live organizations, a status badge, and a progress bar when there is at least one visible task. The progress denominator includes done tasks and excludes canceled tasks because those have already been filtered by the DAO. [Card calculations](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L85-L114)

Tapping a project opens its editor. There is no separate project-detail route, per-project task filter, project note editor, project-type editor, priority editor, deadline control, repository-URL control, or attachment area. Desktop fields can survive inside `remoteJson` without receiving an Android UI. [Project editor controls](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L274-L335) [Project outbound fields](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L114-L131)

## Organization workflow

Choose New organization on Organizations or Dashboard, supply a nonblank trimmed name, and Create. Tapping a card allows rename or Delete. The organization screen reports the number of visible projects whose `organizationId` matches the organization, then counts nondone visible tasks in those projects. It does not count archived projects. There is no membership, permission, invitation, billing, or user-assignment behavior attached to an organization record. [Organization screen](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L144-L200) [Name editor](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L339-L375)

To move a project, edit the project and select a live organization. A missing organization name may indicate an archived parent even though the project remains present. The editor's fallback display text “Personal” does not necessarily mean the stored ID has been repaired: save replaces only a null ID, not a non-null archived or missing parent. [Organization selector](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L293-L315) [Project save fallback](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L100-L113)

## Archive and visibility matrix

| Action | Entity mutation | UI visibility effect | Child mutation | Event |
|---|---|---|---|---|
| Delete task | status canceled, updatedAt now | Task observer hides it | None | `task` / `archived` |
| Delete project | status archived, archivedAt/updatedAt now | Project hidden; ViewModel hides tasks whose parent ID is no longer live | Tasks are unchanged | `project` / `archived` |
| Delete organization | archivedAt/updatedAt now | Organization hidden; projects can remain visible with unresolved parent label | Projects/tasks unchanged | `organization` / `archived` |
| Undo task delete | Original snapshot saved | Reappears if its project is live | None | Normal task save event |
| Undo project delete | Original snapshot saved | Project and otherwise visible children reappear | None | Project updated |
| Undo organization delete | Original snapshot saved | Organization label/counts return | None | Organization updated |

The archive behavior comes from repository methods; the asymmetric parent visibility comes from the DAO filters and `MainViewModel`. [Archive mutations](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L92-L141) [Visible entities](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L119-L162) [Task parent rule](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L22-L26)

No archive action physically erases business records. The sync snapshot includes archived organizations, projects, and canceled tasks. “Delete” therefore means removal from the active UI with propagated retained state. It does not meet an erasure request on its own and should not be described as a secure wipe. [Snapshot of all rows](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L196-L219)

## Undo and restoration limits

Project and organization deletion show a short snackbar with Undo. Undo calls the same normal save method with the pre-deletion entity. There is no confirm-delete dialog or persistent archive screen in these paths. Once the snackbar is gone, the Android UI has no general restore control. A support recovery may require a compatible desktop client, a reviewed data-repair tool, or restoration from a verified backup; those workflows must be validated separately against the authoritative dataset. [Project undo](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L128-L137) [Organization undo](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L187-L198) [Snackbar behavior](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L91-L102)

Do not repair this by blindly editing Room while synchronization is active. Direct SQL can change visible data without emitting the event other devices require, and a stale remote event or later snapshot may reverse the apparent repair. A legitimate repair should use repository mutations or an explicit, reviewed event-producing migration. [Data integrity](11-repository-and-data-integrity.md) explains the invariants.

## Duplicate defaults and slugs

Because fallback matching is name-based and local, independent devices can create their own Personal and Inbox records before syncing. The Android mapper derives a slug from the name plus the first six alphanumeric ID characters when no remote slug is available. This reduces collisions between common names, but neither the name nor the suffix is a database uniqueness guarantee in Android. Existing remote slugs are retained on ordinary edits. [Slug generation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L57-L81) [Organization/project slug use](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L98-L131)

**Recommendation for maintainers:** decide whether duplicate default records should be user-visible, merged through a deliberate migration, or resolved through stable account-scoped IDs. The current client contains no deduplication-by-name synchronization step. Do not document duplicate names as impossible.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
