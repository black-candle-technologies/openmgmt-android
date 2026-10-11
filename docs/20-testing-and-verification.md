# Test catalog, reproducible checks, and verification evidence

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Actual review evidence

The complete pinned source tree was retrieved through authenticated GitHub file reads. Each file was converted back to UTF-8 bytes and checked against the manifest size and Git blob hash computed as SHA-1 of `blob <length>\0<bytes>`. All 50 files matched. Every one of the resulting 5,305 lines was read, including imports, comments, resources, workflow steps, and the existing test. This establishes source coverage and integrity, not application correctness.

| Check | Result during this review | Limit |
|---|---|---|
| Tracked source retrieval | 50/50 files, 208,707 bytes | Pinned commit only; not an open-PR review |
| Git blob SHA and byte size | 50/50 matched | Integrity check, not vulnerability scan |
| XML parsing | 9/9 documents parsed | Does not validate Android resource linking or manifest semantics |
| Test inventory | Seven `@Test` methods in one Kotlin file | Inventory only, not executed-test count |
| Java runtime | OpenJDK 17.0.20 available | `javac` absent |
| Gradle/Kotlin compiler | Absent | JVM test task cannot launch |
| Android SDK, SDK manager, adb | Absent | No Android build, lint execution, install, or emulator test |
| Existing unit task attempt | Exit 127: `gradle: command not found` | No tests ran; no Gradle report exists |

Command attempted in the source checkout:

```text
gradle testDebugUnitTest --stacktrace
/bin/bash: line 1: gradle: command not found
```

The review did not modify source code to make tests pass and did not replace Kotlin execution with a different-language reimplementation. No live OAuth account or production sync dataset was exercised. Build instructions are provided for a configured workstation in [setup](02-build-and-development-setup.md).

## Existing test suite: `PkceTest`

The only tracked test file is `app/src/test/java/com/openmgmt/android/auth/PkceTest.kt`. It imports JUnit4 assertions and `@Test`, tests pure JVM helpers and OAuth defaults, and requires no Android framework in those methods. JUnit dependency is 4.13.2. [Test file](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/test/java/com/openmgmt/android/auth/PkceTest.kt#L1-L68) [JUnit dependency](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L79-L79)

| Exact test method name | Assertion | What it does not establish |
|---|---|---|
| `codeChallenge matches RFC 7636 Appendix B test vector` | Known verifier produces known S256 challenge | Full browser/code exchange interoperability |
| `codeChallenge is deterministic` | Same verifier yields equal outputs | Server acceptance or entropy quality |
| `codeVerifier is 43 url-safe chars` | Length 43; regex A–Z/a–z/0–9/underscore/hyphen | Exhaustive generation validation |
| `codeVerifier is unique per call` | Two generated verifiers differ | Formal collision bound or a large randomized campaign |
| `codeChallenge output is 43 url-safe chars` | Challenge format/length | Token endpoint validation |
| `state is 22 url-safe chars and unique` | Length/format and two samples differ | State-consumption lifecycle or concurrent callbacks |
| `OAuthConfig defaults match the deployed auth service` | Issuer, redirect, and scope equal literals | Any network check of the deployed service |

[Vector/determinism tests](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/test/java/com/openmgmt/android/auth/PkceTest.kt#L18-L29) [Verifier/challenge tests](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/test/java/com/openmgmt/android/auth/PkceTest.kt#L31-L50) [State/default tests](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/test/java/com/openmgmt/android/auth/PkceTest.kt#L52-L67)

The final test's name refers to deployment, but the method constructs OAuthConfig and compares constants. It must not be cited as proof that production DCR, callback policy, or token rotation works. The source deliberately uses `java.util.Base64` instead of `android.util.Base64` so PKCE stays locally testable on the JVM. [PKCE portability](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/Pkce.kt#L10-L24)

## Existing CI gates

The CI workflow has independent Build, Unit tests, Lint, and Release build jobs. Unit tests run `gradle testDebugUnitTest --stacktrace`. A following Python step parses `TEST-*.xml`, sums the tests attribute, and fails if zero tests ran. That guards against a false-green empty suite; it does not establish any minimum coverage outside PKCE or verify that the seven expected test names ran. [CI unit gate](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L54-L93)

Lint runs `gradle lintDebug --stacktrace` and uploads HTML even on failure. Release build runs assembleRelease, which exercises release compilation/minification but does not run device behavior or verify a signature. A workflow file's existence is not a fetched successful workflow run; this review did not retrieve current Actions results. [Lint/release jobs](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L95-L152)

## Reproduce on a configured workstation

After installing the pinned JDK/Gradle/SDK baseline, use:

```bash
gradle testDebugUnitTest --stacktrace
gradle lintDebug --stacktrace
gradle assembleDebug --stacktrace
gradle assembleRelease --stacktrace
```

Inspect test XML under `app/build/test-results/testDebugUnitTest`, not only the terminal's success line. Confirm the count is nonzero and the actual methods are the intended suite. Open `app/build/reports/lint-results-debug.html`; distinguish warnings from failures and inspect the relevant source path. For release validation, install a properly signed minified test build and exercise JSON serialization and encrypted preferences instead of assuming debug success covers R8.

## Test coverage absent from the repository

There are no tracked tests for Room migrations, repository mutation/event atomicity, default-parent creation, archive visibility, sync mapping, replay idempotency, conflict protection, HTTP response errors, partial acceptance, OAuth state/callback handling, refresh rotation, preference migration, sign-out/account switching, Compose UI, accessibility, or backup/restore. There is no `app/src/androidTest` implementation, emulator workflow, MockWebServer dependency, property-testing library, or test fixture directory. These observations follow from the full-file inventory, not a coverage percentage calculation.

## Recommended focused test plan

| Area | Highest-value test | Failure it resolves |
|---|---|---|
| Repository transaction | Force event serialization/write failure and verify entity rollback | Unlogged local mutation |
| Task timestamps | Enter/leave/re-enter done and in_progress | Ambiguous completed/start semantics |
| Defaults | First task, renamed Personal, archived Inbox, null and missing parents | Incorrect automatic parent creation |
| Migration | Real v2 fixture → v3 Room validation and data assertions | Schema/default mismatch or loss |
| Mapping | Unknown fields retained through Android edits | Desktop metadata loss |
| Mapping conflict | Remote new field arrives while local event pending | Unintended full-entity overwrite |
| Replay | Same page twice, own echo, malformed event, unsupported event | Dedupe/skipping behavior |
| Checkpoint | Failure before/after page commit | Lost or repeated remote updates |
| Push | Partial acceptance, unknown accepted ID, lost response | Outbox loss or duplicate delivery |
| Auth | Invalid state followed by valid callback | Pending flow consumption bug |
| Auth | Delayed response after sign-out/new attempt | Stale session resurrection |
| Refresh | invalid_grant versus HTTP 500 versus network exception | Unnecessary session destruction |
| Account switch | A data retained, sign in B, first sync | Implicit cross-account data transfer |
| Backup | Restore records with unavailable credential key | Startup crash/data-loss recovery |
| Time | DST, local midnight, timezone changes | Incorrect daily sections |
| Release | Minified build serializes all request/response types | R8-only failure |

This is proposed work ordered around concrete unresolved risks. It should not be represented as a test suite already supplied by the repository.

## Testability improvements

AuthManager and OmgpClient construct OkHttp internally, use concrete platform preferences, and expose limited injection. SyncManager depends on concrete repository/auth/credential classes; clocks are direct system calls. Focused testing can benefit from injected HTTP calls, clock/zone providers, credential interfaces, and a test database factory. Preserve behavior while introducing those seams; avoid a broad architecture rewrite merely to unit-test immutable UI constants.

The strongest next validation would cover account transfer and credential recovery first, then event/migration correctness, then representative UI interactions. A larger number of shallow tests is less useful than a small suite proving these storage and identity boundaries.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
