# Schedule, Daily Operations, and time semantics

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Dates are task data, not scheduled execution

The local task model contains a nullable `dueAt` epoch-millisecond timestamp. It also retains start/completion timestamps and may preserve desktop scheduling fields inside `remoteJson`, but the Android Schedule and Daily Operations screens organize work solely by `dueAt`. Choosing `scheduled` status is independent from assigning a due date. No notification, alarm, calendar provider, recurring-job engine, or background scheduling code appears in the repository. [Task timestamps](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L64) [Schedule implementation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L25-L45) [Task JSON defaults](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L134-L153)

## Local date conversion

`startOfDay(date)` uses `date.atStartOfDay(ZoneId.systemDefault())` and converts the resulting instant to epoch milliseconds. `localDate(timestamp)` converts an instant through the same current device zone. Today and Tomorrow chips use calendar-day values from `LocalDate.now()`, then store local midnight. [Date helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L349-L374) [Quick due selection](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L257-L269)

The Material date picker represents its selected date as UTC midnight. The app converts an existing due timestamp to its local date, initializes the picker with that date's UTC midnight, then converts the selected UTC day back into a local-start-of-day timestamp. This prevents the naive UTC-to-local conversion from choosing the previous displayed day in negative UTC offsets. It does not attach a permanent timezone to the task. [Picker conversion](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L328-L346)

A stored `dueAt` is an instant. If the phone's timezone changes, the local date derived from that instant may change. A due timestamp received from another client can include a time of day, which Android displays only as a date. Choosing a new date normalizes it to the phone's local midnight; editing notes without touching Due preserves the existing timestamp. These are consequences of the timestamp representation and editor copy behavior.

## Overdue rule

A task is overdue when all three conditions hold:

1. It has a due timestamp.
2. Its status is not done.
3. The timestamp is less than the current local start of today.

A task due earlier today is therefore not overdue according to this app, even if a desktop assigned a precise morning deadline that has passed. A done task is never styled overdue. Canceled tasks are absent from the UI lists before this helper is called. [Overdue helper](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L349-L358) [Task DAO](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L119-L125)

## Schedule section boundaries

Let `T` be today's local midnight in epoch milliseconds, `D = 86,400,000`, and `due` be a task's `dueAt`. Schedule first excludes done tasks, then applies these partitions:

| Section label | Exact condition | Sort |
|---|---|---|
| Overdue | due exists and `due < T` | Due ascending |
| Today | due exists and `T <= due < T + D` | Input order from task observer |
| Next 7 days | due exists and `T + D <= due < T + 7D` | Due ascending |
| Later | due exists and `due >= T + 7D` | Due ascending |
| Unscheduled | due is null | Input order from task observer |

The window labeled Next 7 days excludes Today and ends before midnight seven fixed 24-hour periods from `T`; on an ordinary day it includes the following six calendar dates. It is not a Monday-to-Sunday week and not seven complete days after tomorrow. [Schedule boundaries](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L31-L41)

If there are no open tasks, Schedule shows “Nothing on the schedule.” If there are open tasks but no overdue/today work, those two sections get empty-state messages. Other empty sections are omitted. The helper omits an empty section when no `emptyTitle` is supplied, including the no-open-task path. [Schedule section arguments](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L52-L84) [Section omission](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskSections.kt#L19-L44)

## Daily Operations calculations

Daily Operations builds `open` from nondone visible tasks. It sorts overdue tasks by due date, builds dueToday from open tasks that are not overdue and have due timestamps before `startOfToday() + D`, then concatenates overdue and dueToday. Coming up selects tasks at or after that boundary, sorts by due, and shows the first five. DueToday retains the source observer order; it is not independently sorted by priority or due time. [Daily selection](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L38-L45)

The hero's focus count includes overdue tasks as well as today's tasks. A message such as “4 tasks need you today (2 overdue)” can therefore represent two overdue and two current-day items. “Open overall” counts every visible nondone task, including undated work and work far in the future. Add for today creates a due timestamp but leaves status at the task model default, inbox. [Hero count](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L64-L80) [New-today draft](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L47-L49)

## Dashboard date use

Dashboard attention ordering is overdue first, in progress second, then all other dated open work, with duplicate IDs removed and a six-item limit. A far-future dated task can appear if there are fewer urgent candidates. A high-priority undated ready task will not appear merely because its priority is high; priority is not part of this computation. [Attention calculation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt#L67-L76)

## Formatting reference

| Context | Format |
|---|---|
| Due today | `Today` |
| Due next local day | `Tomorrow` |
| Due previous local day | `Yesterday` |
| Other date in current year | `EEE, MMM d` |
| Date in another year | `MMM d, yyyy` |
| Full date in Daily Operations | `EEEE, MMMM d`, then uppercased by the hero |
| Last-sync time | `h:mm a` in the current local zone |
| Outgoing sync timestamp | `Instant.ofEpochMilli(...).toString()` |
| Incoming sync timestamp | `OffsetDateTime.parse(...).toInstant().toEpochMilli()` |

The outgoing wire format is an ISO instant with UTC `Z`; incoming values are parsed as offset timestamps. Submillisecond precision is lost on conversion to the local `Long` model. There is no custom tolerance for malformed dates; parsing failures in an entity cause that remote event to be skipped/marked applied by the replay layer. [UI format helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L363-L377) [Last-sync formatting](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SyncScreen.kt#L171-L173) [Wire timestamp helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L51-L55) [Replay error handling](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L244-L255)

## Daylight-saving and clock limits

**Source-derived limitation:** the screens use fixed 24-hour millisecond additions for tomorrow and the next-week boundary, whereas the date selector uses actual local calendar-day starts. On a daylight-saving transition, a local day can differ from 24 hours. The selection and grouping can consequently disagree at boundary instants. This is an inference from the two arithmetic approaches, not a reproduced device failure.

The calculations also run during composition with `LocalDate.now()`/current clock calls; there is no midnight timer or clock flow. A screen left untouched over midnight is not guaranteed to reclassify tasks until a recomposition occurs. Changing timezone or the system clock can likewise affect grouping and timestamp generation. [Static composition calculations](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L31-L45) [Schedule time arithmetic](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L31-L41)

**Recommendation:** introduce an injectable clock/zone abstraction, use `LocalDate.plusDays(...).atStartOfDay(zone)` for each boundary, and observe day/zone changes. Before changing behavior, add tests for spring-forward, fall-back, zone changes, exact-midnight timestamps, and the intended definition of “Next 7 days.” Those changes are not implemented in the reviewed source.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
