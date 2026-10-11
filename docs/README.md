# OpenMGMT Android — implementation wiki

This wiki documents the complete tracked implementation of **OpenMGMT Android 0.3.3 (version code 7)** at the immutable commit below. It is intended for end users, contributors, maintainers of the authentication and synchronization services, release engineers, and people diagnosing synchronization or data-retention problems. The application is a native Kotlin/Jetpack Compose Android client backed by a local Room database. Its network integration is a native OAuth sign-in followed by device-token authentication to the `omgp/1` sync service. [Application version and build](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L9-L19) [Composition root](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/OpenMgmtApp.kt#L21-L30)

## Scope and evidence

| Item | Reviewed value |
|---|---|
| Repository | `black-candle-technologies/openmgmt-android` |
| Branch observed | `main` |
| Immutable commit | `e53308af3921f454be1c661691b1fe546cb8f679` |
| Commit timestamp | 2026-09-24 06:14:38 UTC |
| Git tree | `b740e77babef0a32a31002d9c6663b5e2b4e5a3e` |
| Review date | 2026-10-11 UTC |
| Tracked files | 50; all are text at this snapshot |
| Source bytes | 208,707 |
| Lines examined | 5,305, including blank lines, imports, comments, resources, workflows, and the license |
| Retrieval integrity | All 50 files matched both their Git blob SHA-1 and manifest byte length |
| Runnable validation | All nine XML documents parsed successfully; seven existing JUnit test methods were cataloged |
| Build/test limitation | `gradle testDebugUnitTest --stacktrace` could not start: Gradle is absent; the review environment also lacks a Java compiler, Kotlin compiler, Android SDK, and adb |

These counts are from the complete pinned tree and byte-verified local source inventory, detailed in [the coverage ledger](24-source-coverage.md). This is a source review, not a claim that an Android build, emulator run, live sign-in, live sync, release signature, or production deployment was verified. The repository contains no tracked wrapper scripts or wrapper JAR; it contains only `gradle/wrapper/gradle-wrapper.properties`. Build commands in this wiki are instructions to execute in a configured development environment.

**Reading conventions:** “implemented” describes the inspected code; “inference” describes a consequence of that code; “recommendation” is work proposed for maintainers. A source comment claiming desktop equivalence is not treated as a cross-repository compatibility test. Pinned source links let a reader audit each implementation claim without depending on a moving branch. External Android documentation is used only for the few platform behaviors that require it.

## Start here

| Goal | Pages |
|---|---|
| Understand what the app actually offers | [Product overview](01-product-overview.md), [screen reference](04-navigation-and-screens.md) |
| Build and install | [Build setup](02-build-and-development-setup.md), [installation and first run](03-installation-and-first-run.md) |
| Use it daily | [Tasks](05-task-user-guide.md), [projects and organizations](06-structure-and-archiving.md), [dates and planning](07-schedule-and-time-semantics.md), [sign-in and sync](08-account-and-sync-user-guide.md) |
| Understand the engineering | [Architecture](09-architecture-and-lifecycle.md), [database](10-database-and-dao-reference.md), [repository mutations](11-repository-and-data-integrity.md) |
| Integrate the services | [OAuth](13-oauth-and-credentials.md), [wire protocol](14-omgp-protocol-reference.md), [sync algorithm](15-sync-engine-and-conflicts.md), [entity mapping](16-entity-json-interoperability.md) |
| Operate and support it | [Migration and recovery](12-migrations-backup-and-recovery.md), [CI and releases](21-ci-release-and-dependencies.md), [troubleshooting](22-troubleshooting-runbooks.md) |
| Extend or review it | [Security](17-security-and-hardening.md), [Compose design](18-compose-design-and-accessibility.md), [extension cookbook](19-developer-extension-cookbook.md), [tests](20-testing-and-verification.md) |
| Audit documentation accuracy | [Known limits and corrections](23-known-limitations-and-corrections.md), [every-file ledger](24-source-coverage.md) |

## Behavior that should shape every deployment

1. **Local data belongs to the installation, not to an account partition.** Signing out leaves organizations, projects, tasks, archives, and pending history in Room. The disconnect performed during sign-out marks a full snapshot pending. A later sync under newly registered account credentials materializes the retained rows before pull and attempts to push them; transfer depends on successful requests and server acceptance. Do not treat sign-out as a device handover or data-erasure operation. [Disconnect contract](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L50-L58) [Snapshot construction](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L184-L219)
2. **Synchronization is manually initiated.** The app exposes “Sync now”; it does not schedule background sync, pull continuously, or sync each edit automatically. A top-bar “Up to date” label records a successful sync in the current ViewModel lifetime and does not inspect the pending event count. [Sync triggers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L71-L94) [Status pill](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L276-L288)
3. **The schedule is a due-date list.** It does not create reminders, time blocks, recurring work, or a calendar integration. Some desktop-only fields survive round trips inside `remoteJson`, which is different from Android presenting controls for them. [Schedule partitions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L23-L45) [Task mapping](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L134-L153)
4. **The README's URL-client-metadata setup note is stale relative to code.** The implementation uses dynamic client registration at `/oauth/register` and stores the resulting `bct_` identifier. [Actual registration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L200-L229) [README note](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L43-L46)
5. **Credential encryption is narrower than database encryption or backup policy.** OAuth and device credentials use encrypted preferences; the Room database uses the normal Room builder, and the manifest enables backup without explicit rules. [Credential storage](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L61-L72) [Room builder](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L283-L294) [Manifest](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/AndroidManifest.xml#L6-L12)

## Complete page list

1. [Product overview and feature matrix](01-product-overview.md)
2. [Build and development setup](02-build-and-development-setup.md)
3. [Installation and first run](03-installation-and-first-run.md)
4. [Navigation and screen reference](04-navigation-and-screens.md)
5. [Task user guide](05-task-user-guide.md)
6. [Projects, organizations, and archiving](06-structure-and-archiving.md)
7. [Schedule and time semantics](07-schedule-and-time-semantics.md)
8. [Account and sync user guide](08-account-and-sync-user-guide.md)
9. [Architecture and runtime lifecycle](09-architecture-and-lifecycle.md)
10. [Database schema and DAO reference](10-database-and-dao-reference.md)
11. [Repository mutations and data integrity](11-repository-and-data-integrity.md)
12. [Migrations, backup, and recovery](12-migrations-backup-and-recovery.md)
13. [OAuth and credential lifecycle](13-oauth-and-credentials.md)
14. [OMGP wire protocol reference](14-omgp-protocol-reference.md)
15. [Sync engine and conflict behavior](15-sync-engine-and-conflicts.md)
16. [Entity JSON and desktop interoperability](16-entity-json-interoperability.md)
17. [Security boundaries and hardening](17-security-and-hardening.md)
18. [Compose design and accessibility](18-compose-design-and-accessibility.md)
19. [Developer extension cookbook](19-developer-extension-cookbook.md)
20. [Test catalog and verification evidence](20-testing-and-verification.md)
21. [CI, releases, signing, and dependencies](21-ci-release-and-dependencies.md)
22. [Troubleshooting runbooks](22-troubleshooting-runbooks.md)
23. [Known limits and documentation corrections](23-known-limitations-and-corrections.md)
24. [Complete source coverage ledger](24-source-coverage.md)

No repository files, issues, pull requests, production services, or user accounts were changed to produce this wiki.
