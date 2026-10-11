# CI, release signing, build outputs, and dependency inventory

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Workflow inventory

Two workflows build debug APKs. Build APK runs on push to main and manual dispatch. CI runs on pull requests, push to main, and manual dispatch; it has independent Build, Unit tests, Lint, and Release build jobs. Pushes to main therefore match both workflows. This is configuration behavior, not a report that any particular run succeeded. [Build trigger](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/build.yml#L1-L6) [CI trigger/jobs](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L1-L25)

| Workflow/job | Runner / timeout | Main command | Published artifact |
|---|---|---|---|
| Build APK / build | ubuntu-latest / 15 min | gradle assembleDebug --stacktrace | openmgmt-debug-apk |
| CI / Build | ubuntu-latest / 15 min | gradle assembleDebug --stacktrace | openmgmt-debug-apk |
| CI / Unit tests | ubuntu-latest / 15 min | gradle testDebugUnitTest --stacktrace, then XML count guard | No explicit test-report artifact step |
| CI / Lint | ubuntu-latest / 15 min | gradle lintDebug --stacktrace | lint-report HTML, always attempted |
| CI / Release build | ubuntu-latest / 15 min | gradle assembleRelease --stacktrace | No release artifact upload step |

All jobs install Temurin JDK 17 and Gradle 8.7, then use `ANDROID_HOME=/usr/local/lib/android/sdk`. The workflow relies on the hosted runner's SDK installation; it does not run an explicit sdkmanager package-install step. CI concurrency groups by workflow and ref and cancels an earlier in-progress run in that group. The standalone Build APK workflow has no equivalent concurrency setting. [CI concurrency](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L9-L11) [Build environment](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/build.yml#L25-L45)

## Actions supply-chain controls

| Action | Pinned commit in both workflows | Source comment label |
|---|---|---|
| actions/checkout | `3d3c42e5aac5ba805825da76410c181273ba90b1` | v7.0.1 |
| actions/setup-java | `de7274f081f381c8f8158605e0321c36c376e2e6` | v6.0.1 |
| gradle/actions/setup-gradle | `9c971963bec38e04b3d30dcc455b5382be2fdbfb` | v6.3.0 |
| actions/upload-artifact | `043fb46d1a93c77aae656e7c1c64a875d1fc6a0a` | v7.0.1 |

The immutable SHAs are the operative references; comment version labels are annotations from source. Workflows request only contents:read and disable persisted checkout credentials. These protections are appropriate because Gradle can execute PR-controlled code. The repository does not declare a publishing token, signing keystore secret, or workflow step that writes GitHub releases. [CI pinned steps](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L13-L52)

Dependabot updates only the `github-actions` ecosystem, weekly, with chore commit prefix and dependencies label. There is no Gradle dependency update entry in this file. No dependency lockfile, Gradle verification metadata, wrapper JAR, or full wrapper scripts are tracked. Full action pins help one layer of the build chain; they do not prove transitive dependency freshness or reproducible byte-identical APK output. [Dependabot config](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/dependabot.yml#L1-L11) [Wrapper properties](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/gradle/wrapper/gradle-wrapper.properties#L1-L7)

## Dependency inventory

Versions below are declared versions. Compose artifacts without explicit versions are managed by BOM `2024.09.00`; exact resolved transitive graphs were not generated during review. [Dependency block](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L52-L80)

| Dependency | Version | Role |
|---|---|---|
| androidx.core:core-ktx | 1.13.1 | Android Kotlin helpers |
| androidx.activity:activity-compose | 1.9.2 | Compose activity/edge-to-edge integration |
| androidx.compose.ui:ui | BOM | Compose UI runtime |
| androidx.compose.ui:ui-graphics | BOM | Graphics/color primitives |
| androidx.compose.ui:ui-tooling-preview | BOM | Preview support |
| androidx.compose.material3:material3 | BOM | Material3 controls/theme |
| androidx.navigation:navigation-compose | 2.7.7 | Declared navigation dependency; current code uses saved-route dispatch |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.5 | ViewModel integration |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.8.5 | Lifecycle coroutines |
| androidx.browser:browser | 1.8.0 | Custom Tabs |
| androidx.room:room-runtime | 2.6.1 | Room database runtime |
| androidx.room:room-ktx | 2.6.1 | Flow/suspend/transaction integration |
| androidx.room:room-compiler | 2.6.1 via KSP | Generated DAOs/database code |
| com.squareup.okhttp3:okhttp | 4.12.0 | Blocking HTTP requests on IO |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.7.3 | Typed protocol/JSON model |
| androidx.security:security-crypto | 1.1.0-alpha06 | EncryptedSharedPreferences and MasterKey |
| androidx.compose.ui:ui-tooling | BOM, debug only | Development tooling |
| androidx.compose.ui:ui-test-manifest | BOM, debug only | Debug test manifest support |
| junit:junit | 4.13.2, unit tests | Existing PKCE/default tests |

The Compose BOM is also added to androidTestImplementation, but there are no tracked instrumentation tests or a complete Compose test suite in the repository. No analytics, crash-reporting, push-notification, dependency-injection, WorkManager, SQLCipher, Retrofit, or image-loading dependency is declared here. This is an inventory statement, not advice to add them.

## Release configuration

Release has minification enabled and uses the platform optimized ProGuard file plus `app/proguard-rules.pro`. Resource packaging excludes `/META-INF/{AL2.0,LGPL2.1}`. BuildConfig and Compose generation are enabled. Namespace/application ID are the same for debug and release; no product flavor or debug application-ID suffix exists. [Module settings](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L9-L50)

R8 rules suppress warnings for compile-time error-prone annotations used through Tink, retain annotation/inner-class attributes, retain companion-bearing class names, and retain serialization factory member names. These rules are intended to protect runtime Kotlin serialization in minified builds. Their presence is not evidence that every request/response/credential path was exercised after minification. [Project R8 rules](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/proguard-rules.pro#L1-L19)

## Signing and distribution

There is no `signingConfigs` block, no release signingConfig assignment, no tracked keystore, and no CI signing step. `assembleRelease` tests creation of the release variant; it does not establish a signed distributable release. The uploaded debug APK is a development artifact. A maintainer must define a controlled signing and publication process before treating CI output as production distribution.

Recommended release procedure:

1. Pin and record source commit, version name/code, dependency resolution, and build environment.
2. Run build/unit/lint gates and inspect nonzero test results.
3. Build a release variant and apply the intended protected signing process.
4. Verify the APK's signer and expected package/version with Android SDK tools.
5. Install the signed minified artifact on a representative test device and verify local persistence, OAuth, initial registration, sync, upgrade, and restore behavior.
6. Publish through the chosen trusted channel with signer/version information and a documented upgrade policy.

Example verification commands, to run against an actual produced signed file:

```bash
apksigner verify --verbose --print-certs path/to/openmgmt-release.apk
aapt dump badging path/to/openmgmt-release.apk
```

These are recommended operator steps, not commands run during this review. No release artifact or signing key was created.

## Upgrade and rollback discipline

VersionCode is currently 7 and versionName 0.3.3. A future release must choose versioning consistent with its distribution channel and signing continuity. Android data preservation depends on more than code version: the Room schema path and backup state matter. Version 2→3 is explicitly supported; version 1 can be destructively migrated. A downgrade from a future database version has no declared handling. [Version config](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L13-L19) [Migration policy](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Database.kt#L290-L293)

Do not recommend uninstall/reinstall as a generic rollback. Unsynced local work, accepted history, credentials, and preserved remote fields can be lost. [Recovery](12-migrations-backup-and-recovery.md) provides a safer evidence-first process.

## Release documentation that remains to be established

The source does not define store availability, artifact retention duration, production signing owner, key-rotation policy, supported upgrade matrix beyond code, production server version compatibility, end-user privacy policy, or minimum passing security tests. Record these as operational decisions in future release documentation rather than inventing values in this wiki. Existing workflow configuration does not prove branch-protection enforcement or organizational policy.


## Source license and release notices

The tracked LICENSE declares the MIT License with copyright attributed to Black Candle Technologies in 2026. Its permission grant covers use, copying, modification, distribution, and related activities subject to carrying the copyright and permission notices in copies or substantial portions. It also supplies warranty and liability disclaimers. The complete license remains the authoritative text for this source; retain it with release-source packages. [Repository license](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/LICENSE#L1-L21)

The source tree contains no generated third-party attribution report or software bill of materials. The dependency list above identifies direct coordinates, but a distributable also includes resolved transitive dependencies. A release process can generate a dependency/license inventory from the actual resolved build and review which notices accompany the APK. That is recommended release work, not an artifact produced by these workflows or this review.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
