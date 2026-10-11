# Complete source coverage and integrity ledger

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Coverage method and scope

All 50 tracked files at commit `e53308af3921f454be1c661691b1fe546cb8f679` were retrieved in full, reconstructed as UTF-8, verified against their Git blob SHA-1 and byte size, and read line by line. This includes 29 Kotlin files (28 production and one test), nine XML files, three Gradle Kotlin scripts, three YAML files, two properties files, one ProGuard file, the README, the license, and the ignore file. No binary blobs, submodules, or generated source outputs occur in this pinned tree.

**Total: 208,707 bytes and 5,305 lines.** Line counts use the file's actual split-line count, including blank/comment/import lines. A checksum proves the retrieved bytes match Git; it does not prove a program is secure or runs correctly. Every entry below was fully read. Its responsibility note and primary wiki link identify where the review was translated into documentation.

The scope is tracked repository source, resources, tests, and configuration. It excludes dependency implementation source not vendored here, generated KSP/R8 output, an APK's merged manifest, remote auth/sync deployments, private build machines, and open pull requests beyond the pinned commit. Those boundaries prevent “every line” from being misread as an audit of all third-party/runtime code.

## File-by-file ledger

### Build, repository metadata, and CI

| Tracked file | Lines | Bytes | Reviewed responsibility and documentation |
|---|---:|---:|---|
| [.github/dependabot.yml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/dependabot.yml#L1-L11) | 11 | 260 | GitHub Actions weekly update policy; no Gradle ecosystem entry. [Wiki](21-ci-release-and-dependencies.md) |
| [.github/workflows/build.yml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/build.yml#L1-L45) | 45 | 1276 | Push/manual debug build, pinned actions, read-only token, APK upload. [Wiki](21-ci-release-and-dependencies.md) |
| [.github/workflows/ci.yml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L1-L152) | 152 | 4587 | Build, nonempty unit-test guard, lint artifact, release-minification job. [Wiki](20-testing-and-verification.md) |
| [.gitignore](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.gitignore#L1-L26) | 26 | 318 | Generated outputs, local SDK settings, signing stores, IDE files; wrapper JAR exception. [Wiki](02-build-and-development-setup.md) |
| [LICENSE](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/LICENSE#L1-L21) | 21 | 1082 | MIT permission/disclaimer text and 2026 Black Candle Technologies attribution. [Wiki](21-ci-release-and-dependencies.md) |
| [README.md](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L1-L57) | 57 | 2351 | Stated maturity, architecture, build instructions, stale URL-client-metadata note. [Wiki](23-known-limitations-and-corrections.md) |
| [app/build.gradle.kts](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L1-L80) | 80 | 2453 | Application identity/version/SDK, Compose/KSP, dependencies, release R8 configuration. [Wiki](02-build-and-development-setup.md) |
| [app/proguard-rules.pro](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/proguard-rules.pro#L1-L19) | 19 | 826 | Tink annotation suppression and Kotlin serializer/name preservation. [Wiki](21-ci-release-and-dependencies.md) |
| [build.gradle.kts](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/build.gradle.kts#L1-L7) | 7 | 323 | Root AGP and Kotlin Android/Compose/serialization plugin pins. [Wiki](02-build-and-development-setup.md) |
| [gradle.properties](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/gradle.properties#L1-L4) | 4 | 133 | 4GB heap/UTF-8, AndroidX, Kotlin style, nontransitive R classes. [Wiki](02-build-and-development-setup.md) |
| [gradle/wrapper/gradle-wrapper.properties](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/gradle/wrapper/gradle-wrapper.properties#L1-L7) | 7 | 250 | Gradle 8.7 distribution URL/cache paths, timeout and URL validation; no checksum. [Wiki](02-build-and-development-setup.md) |
| [settings.gradle.kts](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/settings.gradle.kts#L1-L16) | 16 | 326 | Plugin/dependency repository policy, root project name, single app module. [Wiki](02-build-and-development-setup.md) |

### Android manifest and resources

| Tracked file | Lines | Bytes | Reviewed responsibility and documentation |
|---|---:|---:|---|
| [app/src/main/AndroidManifest.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/AndroidManifest.xml#L1-L36) | 36 | 1545 | Internet permission, backup enablement, application/activity, native OAuth deep link. [Wiki](17-security-and-hardening.md) |
| [app/src/main/res/drawable/ic_launcher_foreground.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/drawable/ic_launcher_foreground.xml#L1-L16) | 16 | 603 | 108dp vector mark, scaled group/path and lime foreground. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml#L1-L6) | 6 | 338 | Adaptive launcher icon background, foreground, and monochrome reference. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml#L1-L6) | 6 | 338 | Round adaptive icon using the same three resource references. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/res/values-night/colors.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/values-night/colors.xml#L1-L4) | 4 | 116 | Dark launch-window background resource. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/res/values-night/themes.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/values-night/themes.xml#L1-L8) | 8 | 393 | Night platform theme and transparent system bars. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/res/values/colors.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/values/colors.xml#L1-L5) | 5 | 173 | Launcher green and light launch-window color resources. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/res/values/strings.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/values/strings.xml#L1-L4) | 4 | 110 | Single app-name string; no broader localization catalog. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/res/values/themes.xml](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/res/values/themes.xml#L1-L9) | 9 | 481 | Light platform theme and transparent system bars. [Wiki](18-compose-design-and-accessibility.md) |

### Application, auth, storage, and sync

| Tracked file | Lines | Bytes | Reviewed responsibility and documentation |
|---|---:|---:|---|
| [app/src/main/java/com/openmgmt/android/MainActivity.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L1-L140) | 140 | 6406 | Activity lifecycle, edge-to-edge setup, route dispatch, ViewModels, OAuth callback gate. [Wiki](09-architecture-and-lifecycle.md) |
| [app/src/main/java/com/openmgmt/android/OpenMgmtApp.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/OpenMgmtApp.kt#L1-L32) | 32 | 886 | Application composition root for database, repository, auth, credentials, sync. [Wiki](09-architecture-and-lifecycle.md) |
| [app/src/main/java/com/openmgmt/android/auth/AuthManager.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L1-L321) | 321 | 12982 | DCR, browser wait, callback processing, encrypted OAuth state, code/refresh requests. [Wiki](13-oauth-and-credentials.md) |
| [app/src/main/java/com/openmgmt/android/auth/OAuthConfig.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/OAuthConfig.kt#L1-L17) | 17 | 698 | Issuer, native redirect, scope constructor defaults. [Wiki](13-oauth-and-credentials.md) |
| [app/src/main/java/com/openmgmt/android/auth/Pkce.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/Pkce.kt#L1-L32) | 32 | 1010 | Secure-random verifier/state and SHA-256 base64url challenge helpers. [Wiki](13-oauth-and-credentials.md) |
| [app/src/main/java/com/openmgmt/android/data/Database.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L1-L297) | 297 | 10856 | All six Room entities, every DAO query, sync keys, migration 2→3, versioned builder. [Wiki](10-database-and-dao-reference.md) |
| [app/src/main/java/com/openmgmt/android/data/Repository.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L1-L291) | 291 | 13508 | Transactional mutations/outbox, fallback parents, archives, snapshots, replay/checkpoints. [Wiki](11-repository-and-data-integrity.md) |
| [app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt#L1-L103) | 103 | 3732 | Blocking JSON HTTP transport, four endpoints, protocol/HTTP error distinction. [Wiki](14-omgp-protocol-reference.md) |
| [app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L1-L116) | 116 | 3972 | Every request/response/auth/event/rejection field and serialized wire names. [Wiki](14-omgp-protocol-reference.md) |
| [app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt#L1-L78) | 78 | 2976 | Encrypted device identity, registration storage/reset, legacy plaintext migration. [Wiki](13-oauth-and-credentials.md) |
| [app/src/main/java/com/openmgmt/android/sync/SyncManager.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L1-L157) | 157 | 6127 | Sync mutex, hello/register/pull/push, re-registration, paging/batching/counters. [Wiki](15-sync-engine-and-conflicts.md) |
| [app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L1-L196) | 196 | 7554 | Timestamp/slug helpers, entity types/operations, all inbound/outbound field mappings. [Wiki](16-entity-json-interoperability.md) |

### UI, navigation, components, screens, and theme

| Tracked file | Lines | Bytes | Reviewed responsibility and documentation |
|---|---:|---:|---|
| [app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L1-L59) | 59 | 2959 | Eager data projections, live-project task filter, action jobs, factory, project-name map. [Wiki](09-architecture-and-lifecycle.md) |
| [app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L1-L118) | 118 | 4271 | Account/device state, sign-in/sync/sign-out operations, status/message lifetime. [Wiki](08-account-and-sync-user-guide.md) |
| [app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L1-L407) | 407 | 17097 | Safe insets, shared FAB, drawer/top bar/status pill, responsive gutters, system-bar icons. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/java/com/openmgmt/android/ui/components/Common.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L1-L377) | 377 | 14166 | Sections, metrics/cards/badges, name dialog, plural/date formatting and overdue rule. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt#L1-L408) | 408 | 16217 | Shared task actions, Undo, complete task saver, editor/dates/project selection. [Wiki](05-task-user-guide.md) |
| [app/src/main/java/com/openmgmt/android/ui/nav/Destinations.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/nav/Destinations.kt#L1-L46) | 46 | 1723 | All nine routes, groups, labels, fallback route lookup. [Wiki](04-navigation-and-screens.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt#L1-L84) | 84 | 3818 | Four grouped status selections, context-sensitive new task, due ordering. [Wiki](04-navigation-and-screens.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt#L1-L105) | 105 | 4427 | Today/overdue focus, five upcoming tasks, local-date draft default. [Wiki](07-schedule-and-time-semantics.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt#L1-L229) | 229 | 10478 | Metrics, attention selection/cap, active-project summaries, navigation/quick creation. [Wiki](04-navigation-and-screens.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt#L1-L86) | 86 | 3889 | Exact due-date partitions, fixed-day arithmetic, section omission/empty messages. [Wiki](07-schedule-and-time-semantics.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/SettingsScreen.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SettingsScreen.kt#L1-L76) | 76 | 2973 | Account and device status, sign-out visibility, server and version display. [Wiki](08-account-and-sync-user-guide.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt#L1-L376) | 376 | 15225 | Project/org lists, progress/counts, name/status/parent editing and archive Undo. [Wiki](06-structure-and-archiving.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/SyncScreen.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/SyncScreen.kt#L1-L173) | 173 | 7020 | All account/device/queue/time statuses and action states; shared InfoCard/StatusRow. [Wiki](08-account-and-sync-user-guide.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/TaskListScreen.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskListScreen.kt#L1-L121) | 121 | 4785 | All/status filters, stable due/title ordering, new-task context, FilterRow. [Wiki](05-task-user-guide.md) |
| [app/src/main/java/com/openmgmt/android/ui/screens/TaskSections.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/TaskSections.kt#L1-L58) | 58 | 2093 | Lazy section headers and prefixed task keys; omitted empty sections. [Wiki](18-compose-design-and-accessibility.md) |
| [app/src/main/java/com/openmgmt/android/ui/theme/Theme.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/theme/Theme.kt#L1-L195) | 195 | 6310 | Complete light/dark palette, caution/drawer colors, shapes, typography, theme selection. [Wiki](18-compose-design-and-accessibility.md) |

### Tests

| Tracked file | Lines | Bytes | Reviewed responsibility and documentation |
|---|---:|---:|---|
| [app/src/test/java/com/openmgmt/android/auth/PkceTest.kt](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/test/java/com/openmgmt/android/auth/PkceTest.kt#L1-L68) | 68 | 2267 | All seven JUnit methods covering helper vector/format/uniqueness and local config literals. [Wiki](20-testing-and-verification.md) |

## Complete blob integrity ledger

The values below are the exact Git blob hashes verified against the recovered bytes, not commit hashes. The immutable commit/tree links in the home page identify the collection in which these blobs were reviewed.

| File | Verified Git blob SHA-1 |
|---|---|
| `.github/dependabot.yml` | `accb46894dfd1c1763441adca01a57f4eb291f19` |
| `.github/workflows/build.yml` | `5710e2e0141286e29c47a33af6a27b68c76507a3` |
| `.github/workflows/ci.yml` | `86ee76a0802b0f2ce588e249910659cb9a9f3382` |
| `.gitignore` | `bdad2d43e5374052c93b6eb9a488789139018caf` |
| `LICENSE` | `34c39d19bcc12abac9552003315ada7ed5a2a035` |
| `README.md` | `415d6975685af20fcef226ed459eaef5084c74b7` |
| `app/build.gradle.kts` | `51336ad6a8e032e22c25e8f3c324d01c521b5d3d` |
| `app/proguard-rules.pro` | `d5d07014d0687dc7c65bda16b368b022bb08b20b` |
| `app/src/main/AndroidManifest.xml` | `8923f08a19596ae1e08cd4dd0eca3505968a691c` |
| `app/src/main/java/com/openmgmt/android/MainActivity.kt` | `9527de3ad5a9d57d5c527cfd1e7edb272e6f090f` |
| `app/src/main/java/com/openmgmt/android/OpenMgmtApp.kt` | `81da3bb54c7384cf8028004ca138d0ae89b52288` |
| `app/src/main/java/com/openmgmt/android/auth/AuthManager.kt` | `71d7a7f9559e3c17aca289442bd33b967192a01c` |
| `app/src/main/java/com/openmgmt/android/auth/OAuthConfig.kt` | `afe78d974407747117e60418c8f039c38bce1aa1` |
| `app/src/main/java/com/openmgmt/android/auth/Pkce.kt` | `0027c8961d4d03949c6e17b76991b39e0a1203e7` |
| `app/src/main/java/com/openmgmt/android/data/Database.kt` | `23878c2ddeb3bda95af0459b3842c6bb1d115947` |
| `app/src/main/java/com/openmgmt/android/data/Repository.kt` | `541b58b1194a5ba9eba03de70eeb376d12ff1530` |
| `app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt` | `5cd49df17c3ff59c729d5550fd850c9f34199dd8` |
| `app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt` | `56be1f44b77a7223117de0ae176058802abf3732` |
| `app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt` | `7fdc6ed17d873d7b2058053e942b72441ab77a43` |
| `app/src/main/java/com/openmgmt/android/sync/SyncManager.kt` | `a77e48ed1fe693f1069253e5d64af14710cf65ec` |
| `app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt` | `b5c1dc523bfb100f02249759d9ff51ae3de3ce68` |
| `app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt` | `bd533ae0a83831b1d85249470a50b39ae94f447c` |
| `app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt` | `decead7935beff23ef03d06c462fe0163c807353` |
| `app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt` | `7334aaf0cbbd2dbbed494bdf4881c662608b72fe` |
| `app/src/main/java/com/openmgmt/android/ui/components/Common.kt` | `6e67f98fa7db1b9dc96d8ed10e6383ffe687bf2a` |
| `app/src/main/java/com/openmgmt/android/ui/components/TaskEditor.kt` | `e62d885846bcf6089e041d2c78e4710c3d22b6f9` |
| `app/src/main/java/com/openmgmt/android/ui/nav/Destinations.kt` | `d1eaa9f7cc48201dd5e2870d41b8320d7e17867d` |
| `app/src/main/java/com/openmgmt/android/ui/screens/BoardScreen.kt` | `eb8597a33831bb76ce31ce4bf1f13eff15a0da71` |
| `app/src/main/java/com/openmgmt/android/ui/screens/DailyOpsScreen.kt` | `55d1f45381c5f9cbe466a81f8c34980f9b7bb3fc` |
| `app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt` | `e6510749ae2e5a3adbaafeea32ede55f93cf9008` |
| `app/src/main/java/com/openmgmt/android/ui/screens/ScheduleScreen.kt` | `d58f1411074aa0e1c17467a727020a87caaeca30` |
| `app/src/main/java/com/openmgmt/android/ui/screens/SettingsScreen.kt` | `bbefb91f2058080455365e11202ed394192c7fd3` |
| `app/src/main/java/com/openmgmt/android/ui/screens/StructureScreens.kt` | `e1c9f5ea828238bb97364fe70cb9f6a54ffac99e` |
| `app/src/main/java/com/openmgmt/android/ui/screens/SyncScreen.kt` | `a4ae8daa958fc291cb95399a318952bac5e507ae` |
| `app/src/main/java/com/openmgmt/android/ui/screens/TaskListScreen.kt` | `7e2568359ad6c07117f4a76c264452e4d2c47b7f` |
| `app/src/main/java/com/openmgmt/android/ui/screens/TaskSections.kt` | `b3dc51ecd0f18dc3dd6e0df6973eea03099fb361` |
| `app/src/main/java/com/openmgmt/android/ui/theme/Theme.kt` | `e044a56c0d5c56e24284ef5ece64f6d627ce9466` |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | `1e9d6e3bf0c23db9d22891acbdabec5f8757cd32` |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | `0b0d585e422a706a45720174ace6e805808d67f0` |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | `0b0d585e422a706a45720174ace6e805808d67f0` |
| `app/src/main/res/values-night/colors.xml` | `3088609f0749966073c66042f032c7200b3f49f0` |
| `app/src/main/res/values-night/themes.xml` | `d48a0824a3c9056a3c0aa25b39f9f1e2506f4416` |
| `app/src/main/res/values/colors.xml` | `f4e1dfda05db77c6e724dc562520e19e669204c1` |
| `app/src/main/res/values/strings.xml` | `5d9d44dcab1fa17b742ca2a29f26bcd539cdda4e` |
| `app/src/main/res/values/themes.xml` | `17a642adcc4be97fcd95db24b0a53dc15cd57851` |
| `app/src/test/java/com/openmgmt/android/auth/PkceTest.kt` | `7198001b2d584abb5a8c3e2afdf271d6821a21bf` |
| `build.gradle.kts` | `aa513ace60ba65a933a28f2047b96787ee8efc0c` |
| `gradle.properties` | `f97184bebbc81ef689bced813eb3e53ddedf9fbb` |
| `gradle/wrapper/gradle-wrapper.properties` | `b82aa23a4f05d39d81870f8355ca43324f027298` |
| `settings.gradle.kts` | `1dc1385338d8c4277d312d303852a065d4faa876` |

## Important files that are not in this tree

There is no `gradlew`, `gradlew.bat`, wrapper JAR, AGENTS.md, CONTRIBUTING.md, SECURITY.md, exported Room schema, instrumentation-test directory, signing configuration file, backup-rules XML, network-security-config XML, WorkManager implementation, or app export/import feature. Negative findings were checked against the full pinned manifest and relevant source paths. They are not claims that another branch or future release can never contain such files.

## Verification boundary

Nine XML files parsed successfully. Seven existing JUnit methods were cataloged, but the attempted Gradle test task could not start because Gradle is unavailable; the environment also lacks a Java compiler, Kotlin compiler, Android SDK, and adb. No build, Android lint task, emulator run, minified runtime test, production sign-in, or production synchronization passed during this review. See [verification evidence](20-testing-and-verification.md) for the precise attempted command and follow-up validation plan.

## Documentation maintenance

The linked chapters document implementation rather than merely quoting the original README. They include every entity/DAO, callback and token branch, sync request/response field, event mapping, replay/mutation path, navigation destination, screen control, theme/resource role, workflow step, and existing test. When updating the wiki, regenerate this ledger from the new immutable tree and revisit affected callers, not only changed lines. Mark any new runtime validation separately from static coverage.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
