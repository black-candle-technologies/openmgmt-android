# Build and development setup

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Exact build baseline

Use the versions selected by this repository before attempting an upgrade. The build contains a single `:app` module with Kotlin DSL, AndroidX, Compose, Room code generation through KSP, and Kotlin serialization. Plugin resolution uses Google, Maven Central, and the Gradle Plugin Portal; dependencies use Google and Maven Central. Project-level dependency repositories are rejected. [Settings](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/settings.gradle.kts#L1-L16)

| Component | Source value | Reference |
|---|---|---|
| Android Gradle Plugin | `8.5.2` | [Root plugins](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/build.gradle.kts#L1-L7) |
| Gradle | `8.7` | [Wrapper properties](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/gradle/wrapper/gradle-wrapper.properties#L1-L7) |
| Kotlin Android, Compose, serialization plugins | `2.0.20` each | [Root plugins](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/build.gradle.kts#L2-L6) |
| KSP | `2.0.20-1.0.25` | [App plugins](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L1-L7) |
| Java source/bytecode | `17` | [Compile options](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L43-L49) |
| Android compile/target SDK | `34` / `34` | [Android config](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L9-L19) |
| Minimum device API | `26` | [Android config](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L13-L19) |
| Gradle heap | `-Xmx4g`, UTF-8 | [Gradle properties](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/gradle.properties#L1-L4) |
| Compose BOM | `2024.09.00` | [Dependencies](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L52-L55) |
| Legacy Compose extension field | `1.5.14` | [Compose configuration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L31-L37) |

Google's [AGP 8.5 compatibility documentation](https://developer.android.com/build/releases/agp-8-5-0-release-notes#compatibility) specifies Gradle 8.7, JDK 17, SDK Build Tools 34.0.0, and support through API 34. The app does not explicitly set `buildToolsVersion`; installing 34.0.0 matches that baseline. The Kotlin Compose plugin coexists with the older `composeOptions.kotlinCompilerExtensionVersion` setting. Treat cleanup of that coexistence as a separate build change and validate it; this review did not compile the project.

## Required workstation components

Install a full JDK 17, including `javac`, and Android SDK command-line/platform tools. A Java runtime alone is not a complete developer toolchain. Configure the IDE's Gradle JDK and shell Java selection consistently. Android Studio must support AGP 8.5.2; the README's generic “Hedgehog or newer” phrase does not establish compatibility for every IDE release in that range. Select a compatible IDE or use the command line with the declared baseline. [README prerequisites](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L31-L41)

The workstation needs repository access, a trusted Gradle distribution, and dependency-host access as declared in the build. The production auth/sync hosts are needed only for those integration tests, not for local CRUD after dependencies are installed. No private Maven credentials or app-specific build secrets are present in the scripts.

## Obtain the reviewed revision

These commands are workstation instructions, not operations performed against production during this review:

```bash
git clone https://github.com/black-candle-technologies/openmgmt-android.git
cd openmgmt-android
git checkout --detach e53308af3921f454be1c661691b1fe546cb8f679
java -version
javac -version
gradle --version
```

For changes, create a feature branch from the baseline you intend to modify. Detached checkout is useful for reproducing this wiki's snapshot. Use the normal authenticated Git method if the repository requires it; avoid embedding account tokens in URLs or tracked configuration.

## Important: the wrapper is incomplete in Git

Only `gradle/wrapper/gradle-wrapper.properties` is tracked. `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` are absent. Thus `./gradlew` does not work immediately after a fresh checkout. The README instructs wrapper generation, while CI installs Gradle 8.7 and invokes `gradle` directly. The `.gitignore` permits a future wrapper JAR; that exception is not evidence that the JAR exists. [README instructions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L35-L39) [CI invocation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L38-L46) [Ignore rules](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.gitignore#L1-L12)

With a trusted Gradle **8.7** installation already available:

```bash
gradle wrapper --gradle-version 8.7 --distribution-type bin
./gradlew --version
./gradlew assembleDebug --stacktrace
```

On Windows use `gradlew.bat` after generation. Alternatively, use `gradle assembleDebug` directly, matching CI. The wrapper task can configure the Android project before generating files, so resolve JDK/SDK configuration first if necessary. The distribution URL is `https://services.gradle.org/distributions/gradle-8.7-bin.zip`, its network timeout is 10,000 ms, and URL validation is enabled. No distribution SHA-256 value is tracked. [Wrapper settings](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/gradle/wrapper/gradle-wrapper.properties#L1-L7)

**Recommendation:** commit and verify a complete wrapper, including scripts and JAR, through the normal review process so contributors have a reproducible bootstrap. Do not trust a randomly acquired wrapper binary or unreviewed build plugin merely because it makes setup easier.

## Android SDK configuration

Install required packages using Android Studio's SDK manager or equivalent commands:

```bash
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
sdkmanager --licenses
```

Set the SDK location through Android Studio, `ANDROID_HOME`, or an untracked `local.properties`, using the actual workstation path:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

CI's `/usr/local/lib/android/sdk` is a runner path, not a portable default. Local SDK settings and signing stores are ignored by Git. No NDK, CMake, Rust, Node, or JavaScript dependency installation is declared. KSP generates Room implementations as part of the build; do not hand-create `AppDatabase_Impl`. [CI SDK path](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L43-L46) [Ignored local files](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.gitignore#L9-L12) [KSP dependency](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L68-L70)

## Build and validation commands

Use the existing CI-equivalent commands or substitute a verified generated wrapper:

```bash
gradle assembleDebug --stacktrace
gradle testDebugUnitTest --stacktrace
gradle lintDebug --stacktrace
gradle assembleRelease --stacktrace
```

| Result | Expected location or behavior |
|---|---|
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| JUnit XML | `app/build/test-results/testDebugUnitTest/TEST-*.xml` |
| Android lint HTML | `app/build/reports/lint-results-debug.html` |
| Release build | Minification enabled; project R8 rules included; no signing configuration declared |

These paths and tasks come from CI and the module build script. A release task completing is not proof of a signed artifact or runtime compatibility. [CI artifacts/tests](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L48-L93) [Lint/release](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L116-L152) [Release config](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L21-L29)

To install a debug build on an attached development device or emulator:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.openmgmt.android/.MainActivity
```

The update request does not bypass certificate checks. Preserve local data before replacing builds or changing package identity. See [installation](03-installation-and-first-run.md).

## Configure development services

`OAuthConfig` takes issuer, redirect URI, and scope constructor parameters. `SyncManager` takes a server URL. `OpenMgmtApp` currently wires defaults. There is no `.env`, endpoint preference, Gradle product flavor, or build-config URL override. To test other services, explicitly supply nonproduction values in the composition root and arrange matching server policy. Keep the public-client model: do not embed a client secret. [OAuth defaults](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/OAuthConfig.kt#L12-L17) [Sync default](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L32-L37) [Wiring](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/OpenMgmtApp.kt#L21-L30)

Redirect changes must cover `OAuthConfig`, the manifest filter, `MainActivity`'s hard-coded callback predicate, and relevant default tests. Changing the application ID alone will not update these strings. A side-by-side debug variant also needs deliberate application-ID/callback handling; the current build defines no debug suffix.

## Review environment result

The reviewer had OpenJDK runtime `17.0.20` but no Java compiler, Gradle, Kotlin compiler, SDK manager, Android SDK, or adb. `gradle testDebugUnitTest --stacktrace` exited 127 with `gradle: command not found`; no application code or JUnit tests ran. Nine XML files parsed, and 50/50 source blobs passed SHA/byte checks. These checks do not validate Compose compilation, generated Room schema, browser routing, or R8. [Detailed evidence](20-testing-and-verification.md) separates checks completed from checks still required.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
