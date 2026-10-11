# Navigation and complete screen reference

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Application navigation

The app has nine destinations in three drawer groups. Navigation is implemented by a saved string route and a `when` expression, not a `NavHost` or an accumulating navigation back stack. Unknown route strings resolve to Dashboard. Android Back from a non-Dashboard destination returns to Dashboard; Back while the drawer is open closes the drawer. [Destination definitions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/nav/Destinations.kt#L8-L46) [Route state and Back](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L71-L109) [Drawer Back handling](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L137-L151)

| Drawer group | Label | Internal route | Primary action |
|---|---|---|---|
| WORKSPACE | Dashboard | `dashboard` | New task |
| WORKSPACE | Daily Operations | `daily_ops` | Add for today |
| WORKSPACE | Tasks | `tasks` | New task |
| WORKSPACE | Schedule | `schedule` | New task |
| STRUCTURE | Projects | `projects` | New project |
| STRUCTURE | Organizations | `organizations` | New organization |
| OPERATIONS | Board | `board` | New task in selected board group |
| OPERATIONS | Sync | `sync` | Sign in / Sync now / Sign out, according to state |
| OPERATIONS | Settings | `settings` | Sign out or navigate to Sync |

Every screen uses the shared application shell. The menu button opens a charcoal drawer. The top bar shows an eyebrow and page title, a clickable sync-status pill, and a Sync now icon when synchronization is available. A running sync replaces the icon with progress. The bottom drawer label “Local database · sync on” means credentials permit a sync; it is not a background-worker or connection-health indicator. [Top-bar controls](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L174-L222) [Drawer footer](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L361-L373)

## Dashboard

The six metric cards are In progress, Overdue, Blocked / waiting, Open tasks, Projects, and Orgs. “Open” means a visible task whose status is not done. The projects metric counts only active projects; the organization metric counts nonarchived organizations. Needs attention concatenates overdue tasks sorted by due date, then in-progress tasks, then other dated open tasks sorted by due date, removes duplicate IDs, and keeps six. This is not a priority-ranked or AI-generated recommendation list. [Dashboard calculations](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt#L67-L76)

Metric taps navigate to a destination, but no filter is passed. In progress and Blocked / waiting both open Board, whose initial selection is To do. Overdue opens Schedule. Open tasks opens Tasks, Projects opens Projects, and Orgs opens Organizations. A reader should not interpret the destination's initial rows as already filtered to the metric they tapped. [Metric navigation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt#L85-L113) [Board default](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt#L45-L47)

Active projects shows at most six, preserving the project DAO's name ordering; each row displays the number of visible nondone tasks in that project. Those rows are informational rather than clickable detail links. All projects opens the Projects screen. Quick actions create a project or organization by name; New task opens the common task editor. [Project summary](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt#L142-L180) [Quick creation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt#L183-L210)

## Daily Operations

The hero displays the current local date, a focus count, overdue count when nonzero, and overall open count. Today's focus contains every overdue task followed by tasks due before the computed next-day threshold. Coming up shows the first five future-dated open tasks sorted by due date. Add for today creates a draft with today's local-midnight due timestamp and default Inbox status. There is no separate persistent daily plan or focus assignment. [Daily computations and creation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L38-L49) [Hero and sections](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L56-L103)

## Tasks

The horizontal chip row contains All followed by eight selectable statuses, each with a count. All includes completed tasks but excludes canceled tasks and tasks with non-null parents absent from the live project list. Sorting is nondone first, then earliest due timestamp, then lowercase title; no-date tasks sort last within the done/open grouping. A new task adopts the selected status, including done if the Done filter is selected. [Task filters and order](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskListScreen.kt#L31-L69) [Task visibility](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L22-L26)

Card taps edit; the checkbox completes or reopens. Status badges are hidden when a specific status filter already explains the list. All has no search box, text query, project filter, multi-select, or bulk action.

## Schedule

Schedule partitions visible nondone tasks by `dueAt`: Overdue, Today, Next 7 days, Later, and Unscheduled. The future window is calculated from today's local start plus fixed millisecond intervals. Empty future/unscheduled sections are omitted; Overdue and Today show reassuring empty messages when other open tasks exist. With no open tasks, a single empty-state message appears and the helper omits the empty sections. The implementation comment saying these sections “always show” is qualified by this no-open-task path. [Schedule calculations and sections](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L31-L84) [Empty section helper](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskSections.kt#L19-L44)

Changing a task's status to `scheduled` does not place it in a dated section by itself. The screen uses the due timestamp, not status or preserved desktop scheduling fields. [Time semantics](07-schedule-and-time-semantics.md) contains the exact boundary table.

## Projects

Projects are ordered with active work first; all other live project statuses follow, name-sorted within those broad groups. A card shows name, open-task count, live organization name if found, status badge, and a completion bar when it contains visible tasks. Progress is done count divided by all visible project tasks; it excludes canceled tasks because those never reach the ViewModel list. Tapping opens the project editor, not a project-specific task list. [Project screen](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L74-L139)

New project asks only for a name and saves to the default Personal organization. Edit offers name, active/paused/completed status, and an organization selector. Delete archives the project and provides Undo. The parent has to be changed after creation if Personal is not appropriate. [Project editor](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L257-L336)

## Organizations

Organization cards are name-sorted and show live-project count plus nondone-task count across those projects. New organization asks for a name; tapping an existing card opens a name editor with Delete. There is no settings/permissions detail page behind an organization. Archiving does not cascade to projects or tasks. [Organization screen](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L144-L200) [Organization archive](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L136-L141)

## Board

The board uses four horizontally selectable groups: To do combines inbox/backlog/scheduled/ready; In progress contains in_progress; Blocked combines blocked/waiting; Done contains done. One group's vertical list is shown at a time. Each chip displays the count for its group; rows are sorted by due date with undated work last. Status badges remain visible only in groups containing more than one status. [Board group definitions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt#L25-L37) [Board display](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt#L41-L79)

New tasks in each group default to inbox, in_progress, blocked, or done respectively. To move an existing task, open the editor and choose status. There are no drag handles, move gestures, status columns displayed simultaneously, timers, TV-mode controls, or drag-and-drop code.

## Sync and Settings

Sync displays account state, registration state, last sync this session, and unsent-event count. It also displays the latest operation message and the configured server. Buttons depend on the independent signed-in and registered states; a device with only a device token can still show Sync now and a separate Sign in button. [Sync status/buttons](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SyncScreen.kt#L42-L130)

Settings displays account state, optional sign-out, server hostname, registration state, and version. It is informational; it does not allow endpoint editing, appearance switching, logging controls, backup management, or account selection. [Settings](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SettingsScreen.kt#L36-L74)

## Shared interaction details

Task cards show title, optional status and project badges, and a due-date label. A default Inbox task suppresses a redundant Inbox project badge by identity, rather than suppressing every project whose name resembles a status. Done task titles use a strike-through. Overdue styling applies only before today's start and excludes done tasks. [Card behavior](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L174-L246) [Date helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L349-L374)

Screen-level floating actions are registered through `ScreenFab` and rendered in the shared Scaffold so snackbars sit above them. Content is centered to a 720 dp maximum reading width with at least 16 dp side gutters. System bars and display cutouts are included in insets, and drawer-open state adjusts system-bar icon brightness. These are concrete layout accommodations, not proof that every device/font-scale combination was exercised. [FAB host](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L98-L119) [Responsive content](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L224-L251)

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
