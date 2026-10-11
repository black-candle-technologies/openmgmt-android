# Known limitations, documentation corrections, and unresolved decisions

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Purpose

This page is the maintenance backlog for accurate documentation. It separates an implemented boundary from a misleading description and from proposed engineering work. The goal is to keep the wiki aligned with this pinned source, not to claim that every limitation is a reproduced vulnerability or a defect requiring the same urgency.

## README and comment corrections

| Existing description | More precise documentation | Evidence |
|---|---|---|
| Scaffold | Early client with nine screens, local persistence, native OAuth, and manual sync; maturity remains explicitly early | [README label](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L7-L9), [Screen dispatch](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L92-L109) |
| Register redirect in URL client metadata before sign-in | Code uses DCR `/oauth/register`, caches bct_ client_id, and requests native redirect/public-client grants | [README note](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L43-L46), [Actual DCR](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L203-L229) |
| Generate wrapper JAR once | Fresh checkout lacks scripts as well as JAR; only properties is tracked | [README setup](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L35-L39), [complete file ledger](24-source-coverage.md) |
| Android Studio Hedgehog or newer | IDE must support declared AGP 8.5.2; generic age range is not a compatibility guarantee | [README prerequisites](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L33-L41), [AGP](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/build.gradle.kts#L2-L6) |
| Last write wins in server order | Same-type page order is respected, category order is normalized, pending local entity suppresses incoming state, and no timestamps are compared | [Replay](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L221-L257) |
| Desktop-only fields survive Android round trips | Preserved from the last accepted baseline; concurrent skipped remote events and malformed baseline JSON can lose newer fields | [Base mapping](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L83-L96), [Pending guard](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L244-L251) |
| Nothing hard-deleted | Business records are retained/archived; pending sync-event rows are physically deleted during snapshot replacement | [Snapshot](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L196-L219) |
| Callback must match redirect exactly | Runtime predicate checks only scheme and path, not absent host/port or full configured URI | [Runtime predicate](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L122-L138) |
| Clear pending values to prevent replay | They are removed before state validation, allowing interruption of a legitimate pending attempt | [Callback consumption](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L125-L143) |
| Refresh failure means invalid_grant | Code treats all non-2xx refresh responses as session expiry, including temporary failures | [Refresh branch](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L304-L315) |
| Schedule Overdue and Today always show | Empty sections are omitted when no open tasks and emptyTitle is null | [Arguments](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L52-L72), [Helper](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskSections.kt#L19-L44) |
| OAuth defaults match deployed auth service test | Test compares local literals only; no deployed service request occurs | [Default test](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/test/java/com/openmgmt/android/auth/PkceTest.kt#L61-L67) |

## Implemented product limits

| Area | Current boundary | User/support implication |
|---|---|---|
| Account separation | Single installation-wide DB; sign-out marks a snapshot pending and later sync attempts to push retained data under newly registered account credentials | Sign-out is not independent-profile isolation |
| Synchronization cadence | Manual only | Device changes do not automatically appear elsewhere |
| Sync health | Up to date is current-session success timestamp, not queue state | Check pending events/message before asserting upload completion |
| Task editing | Title, notes, status, due, project only | Priority and several desktop fields cannot be edited on Android |
| Scheduling | Due-date sections | No alarms, recurrence, time blocking, calendar export, or reminders |
| Board | One selected group, no drag/drop | Status changes occur through task editor |
| Project navigation | Card opens editor | No dedicated project task-detail screen |
| Archive recovery | Immediate Undo only | No persistent trash/archive browser |
| Search/bulk actions | Absent | Tasks must be found through available lists/status filters |
| Server configuration | Constructor defaults; read-only display | Endpoint changes require code/configuration work |
| Account identity display | Signed-in Boolean only | No local confirmation of selected account subject/name |
| Data export | Absent | Support backup needs external authorized tooling |

[Editor surface](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L194-L324) [Sync UI/state](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L20-L32) [Settings](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SettingsScreen.kt#L36-L74) [Board](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt#L41-L79)

These limits should appear in product/user documentation before promising full desktop parity. The Android model may carry remote metadata without exposing a control or using it to drive a screen.

## Data and synchronization risks to resolve

| Behavior | Consequence inferred from source | Suggested direction |
|---|---|---|
| No account key on entities/outbox | Cross-account retained-data transfer | Explicit per-account storage or reviewed migration design |
| Malformed/unsupported events marked processed | Data can remain missing after client upgrade/retry | Observable quarantine and replay recovery |
| Pending-local entity skips all remote changes | Concurrent independent field updates can be replaced | Define conflict UX or merge semantics |
| 200-page pull cap proceeds to push | A “successful” run can leave remote pages unread | Return explicit incomplete state/progress |
| No checkpoint progress validation | Repeated server pages consume loop budget | Require forward progress where protocol supports it |
| Old rejected events remain in oldest batch | Newer events can be delayed indefinitely | Reviewed rejection recovery/quarantine strategy |
| All local history retained | Outbox/dedupe growth | Retention compatible with idempotency and recovery |
| No FK constraints or non-null parent validation | Missing/stale parents can hide rows | Validate/reconcile intentional relationships |
| Fixed 24-hour day arithmetic | DST boundary misclassification | Calendar-day boundaries with injected clock/zone |
| No observable midnight tick | Unchanged screen can show stale date grouping | Day/zone-aware state source |

[Data schema](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L47-L117) [Replay/reset](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L184-L289) [Loop behavior](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L115-L153) [Time arithmetic](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L31-L41)

## Security and lifecycle priorities

The most consequential boundaries are account transfer, credential backup recovery, callback transaction consumption, and sign-out/session lifecycle. HTTPS defaults and encrypted token preferences are useful protections, but do not establish whole-database encryption, end-to-end encryption, server authorization correctness, or remote revocation. The security chapter provides precise source evidence and recommendations without claiming exploit verification. [Credential/DB distinction](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L61-L72) [Normal Room builder](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L283-L294) [Sign-out](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L187-L198)

A specific registered-but-not-signed-in state allows sync and sign-in but not sign-out in the current screens. Provide a device-disconnect action independent of account-token presence if operators expect users to stop all synchronization. Correct the help text saying data stays on-device until sign-in in that state: an existing device token already permits synchronization. [Buttons/help text](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SyncScreen.kt#L94-L123)

## Engineering maturity limits

The test suite contains seven PKCE/configuration tests and no coverage for database migrations, repository transactions, network protocol handling, account switching, or UI. CI builds/lints/tests the declared tasks, but this review did not run Gradle or inspect successful hosted results. Release signing and store publication are not configured. Room schema export is disabled, the Gradle wrapper is incomplete, and Dependabot only covers GitHub Actions. [Tests](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/test/java/com/openmgmt/android/auth/PkceTest.kt#L14-L68) [Schema export](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L261-L272) [Dependabot](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/dependabot.yml#L1-L11)

UI lists load all matching records and perform filtering/counting in composition. No performance benchmark establishes a supported task/event scale. There is no telemetry/crash-reporting integration or in-app diagnostic export. Kotlin literals dominate UI strings, and no accessibility/localization test evidence is present. These are product and maintenance gaps, not quantitative performance/security conclusions.

## Decisions that documentation cannot invent

Maintainers still need to establish release artifact provenance, signing ownership/rotation, supported server revision matrix, data-retention policy, account-switch policy, backup inclusion/exclusion, migration support for future releases, invalid-event handling, a privacy/support contact process, and production acceptance tests. The repo contains no source of truth for those organizational decisions. Keep them marked as unresolved until an owner approves concrete behavior and implementation supports it.

## Keep this wiki current

For a later snapshot, record the new commit/tree, re-audit changed files and their callers, rebuild reference tables, and rerun meaningful tests in a configured Android environment. Pay particular attention to auth registration changes, schema versions, parent visibility rules, event operations, new fields, signing paths, and UI status messages. Do not silently carry forward this wiki's limitations after remediation or remove them because a README says the problem was fixed; check the implementation and validation evidence.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
