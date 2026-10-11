# Troubleshooting and diagnostic runbooks

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Start with facts that preserve the dataset

Record the app version, device/API level, operation, exact visible error, approximate time, account-token presence, device registration state, and Changes to send. Check whether the issue is local visibility, local persistence, OAuth, registration, pull, or push. These are different paths. Avoid clear-data, uninstall, direct outbox deletion, or repeated account switching until the local/remote authoritative dataset is understood.

The code uses tag `OpenMGMT` for failed OAuth callbacks and skipped remote events. A developer can collect redacted logs on an authorized test/debug device. Callback URLs, codes, verifier/state, access/refresh/device tokens, full database copies, and entity payloads should not be put in public issue reports. [Callback log](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L129-L134) [Remote log](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L247-L250)

## Build and installation

| Symptom | Likely source-level cause | Next diagnostic/action |
|---|---|---|
| `./gradlew: No such file` | Scripts absent in snapshot | Install trusted Gradle 8.7, generate wrapper or use CI's direct gradle path |
| `gradle: command not found` | System Gradle absent | Install/configure the pinned Gradle distribution |
| Android plugin requires Java 17 | Wrong Gradle JVM | Check shell and IDE Gradle JDK; full JDK 17 required |
| `javac` absent | Runtime-only Java installation | Install full JDK rather than only a JRE |
| SDK location missing | No valid SDK path | Configure local.properties or SDK environment for actual workstation |
| Platform android-34 missing | Required SDK platform absent | Install platform 34 and compatible build tools |
| Dependency resolution fails | Repository/network/cache problem | Inspect failing artifact/host; repository permits declared Google/Maven repositories |
| Zero tests in CI | Test reports missing/empty | Inspect Gradle task and TEST-*.xml; CI intentionally fails zero count |
| R8 missing annotation warning | Security-crypto/Tink annotation dependency path | Verify project ProGuard rules are included; inspect actual release failure |
| Install signature mismatch | Existing app signed differently | Preserve data; obtain compatible signer/build; do not casually uninstall |
| Device API too low | minSdk=26 | Use a supported test device; do not lower SDK without reviewing APIs |

[Toolchain](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/build.gradle.kts#L9-L49) [Workflow commands](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/.github/workflows/ci.yml#L75-L93) [R8 rule](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/proguard-rules.pro#L3-L6)

## OAuth failures

| Visible result or condition | Code path | Recommended investigation |
|---|---|---|
| Client registration failed: HTTP N | `/oauth/register` non-2xx | Auth service availability/policy for public native DCR; not URL metadata setup |
| Invalid client_id after registration | Missing/non-bct_ ID | Inspect redacted DCR response schema and issuer configuration |
| Browser does not return to app | Callback routing mismatch or interrupted browser flow | Verify exact custom scheme/path in config, manifest, and server accepted redirect |
| Waiting for browser remains | Deferred still waiting | Complete browser flow or allow five-minute timeout; browser dismissal is not immediate cancellation |
| Sign-in timed out | No deferred completion within five minutes | Start a new attempt; remember old pending values are not cleared by timeout |
| Invalid OAuth callback | Missing code or mismatched state | Begin a fresh attempt; check duplicate/stale callback and app lifecycle |
| Authorization failed: error | Callback contains error | Investigate issuer-provided error without exposing callback credentials |
| Token exchange failed: HTTP 401 | Code exchange rejected | Client ID is removed locally; next sign-in re-registers; investigate issuer reset/registration policy |
| Token exchange failed: other HTTP | Non-2xx token response | Check code expiry/reuse/PKCE/redirect/client contract; generic UI hides response detail |
| No access token in success JSON | Unexpected token response | Verify response schema; a 2xx alone is not success |
| Session expired after temporary server outage | Any non-2xx refresh clears session | Fresh sign-in under current code; maintainers should classify transient errors |
| Signed in despite old token | isSignedIn checks preference presence | A valid-token request is deferred until registration/re-registration |

[Auth error paths](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L79-L163) [DCR/token errors](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L203-L318)

### OAuth routing investigation

Compare these three local declarations: OAuthConfig's `com.openmgmt.android:/oauth2/callback`, MainActivity's scheme/path predicate, and the manifest filter. Then verify the auth service accepts the native redirect for the issued DCR client. The README's URL-client-metadata instruction is not the current registration path. If testing an artificial deep link, use a disposable flow: a matching invalid callback consumes pending state before validation. Do not inject a fake callback into a user's legitimate active sign-in. [Configuration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/OAuthConfig.kt#L12-L17) [Callback gate](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L122-L138) [Pending consumption](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L125-L143)

## Device registration and transport

| Symptom | Meaning | Next step |
|---|---|---|
| Sign in to register this device | No stored access token when registration required | Sign in first; local edits remain pending |
| Server does not support omgp/1 | Hello compatible=false | Verify service version/route and protocol contract |
| Device registration was not accepted | accepted=false or missing/empty token | Inspect server registration policy/response |
| Server returned HTTP 401 | Generic HTTP failure | Do not expect automatic re-registration; only protocol unauthorized triggers it |
| Protocol unauthorized after retry | Token recovery already used or no signed-in account | Investigate device ownership/account session; fresh registration may need service-side remediation |
| A sync is already running | Manager mutex not acquired | Wait for current run; investigate stalled blocking request if persistent |
| JSON deserialization failure | Empty/malformed response or missing required field | Verify endpoint returns expected model even for error responses |
| Connection/read timeout | Request cannot connect/read within client limit | Check connectivity, DNS/TLS, server latency; avoid clearing local data |

[Client validation/errors](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt#L54-L95) [Registration/recovery](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L73-L110)

## Sync completes but data is missing or pending

| Symptom | Source explanation | Recovery direction |
|---|---|---|
| Up to date, yet Changes to send > 0 | Pill only remembers last successful run | Run sync explicitly; use pending count as current queue signal |
| Last sync becomes Not this session after restart | Timestamp is in-memory ViewModel state | Expected; inspect actual records and pending count |
| Received count smaller than downloaded page count | UI shows applied, excluding echoes/duplicates/conflicts/failures | Inspect replay reasons and pending entity state |
| Pending count larger than task count | Each mutation/default parent is an event | Expected; distinguish history from duplicate entities |
| Rejections repeat indefinitely | Rejected/unacknowledged events remain oldest pending batch | Diagnose rejected payload; no built-in quarantine/drop control |
| Later changes never upload | Old rejected events can occupy first batch | Fix or deliberately repair the offending events through a reviewed process |
| New server entity type never appears | Unsupported type logged and marked processed | Requires compatibility and replay-recovery design; retry alone will not restore it |
| Reset checkpoint still does not replay records | Applied-event IDs are retained | Do not clear dedupe blindly; plan a tested replay repair |
| Another device's field changes disappear | Pending-local full-entity precedence | Reconcile authoritative fields; sync before switching edit devices |
| Large remote history not all seen in one run | 200-page pull cap then push | Repeat only after checking progress; maintainers should expose incomplete pull state |
| Own edits are downloaded again | Push checkpoint is not adopted; own echoes are skipped next pull | Expected identity/checkpoint behavior |

[Pill](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/AppScaffold.kt#L276-L288) [UI counters](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L78-L91) [Replay policy](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L231-L257) [Batch cap and stop](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L115-L153)

### Investigating one pending event

Work from a consistent database copy. Identify the oldest pending event's type, entity ID, operation, and sequence; compare its payload shape with [the mapper reference](16-entity-json-interoperability.md) and the pinned server contract. Check null/nonexistent parents, invalid preserved field types, malformed timestamp text, unsupported operation/type, and server-side authorization. The UI does not display `rejected_events[].error` details, so the server or a redacted test client trace may be needed. Do not mark an event synced merely to reduce the count: that discards its delivery obligation.

## Local data and UI

| Symptom | Expected explanation or code path | Action |
|---|---|---|
| First task created Personal and Inbox | Lazy default parent creation | Expected normal hierarchy |
| Task missing after project deletion | Live-project filter hides children | Undo project archive if available; inspect retained rows |
| Organization deleted but projects remain | No cascading project filter on organization | Reassign/archive projects intentionally; not a hard-delete failure |
| Done task disappears from Daily/Schedule | Those screens filter out done | Open Tasks → Done or Board → Done |
| Task with unknown status absent from Board | Only known status groups included | Inspect/normalize status through intended editor/repository path |
| Due today but not scheduled status | Due field and status are independent | Change status separately if desired |
| Morning deadline passed, not overdue | Overdue is before local midnight today | Expected date-only presentation |
| Dates shift around zone/DST change | Instants plus device zone/fixed-day additions | Capture timestamp/zone; use time semantics when diagnosing |
| Metric opens wrong board group | Navigation does not pass selected filter | Select desired board chip manually |
| Editing fields lost after dismiss | Cancel/swipe dismissal does not commit draft | Reopen and save; rotation preservation is a different mechanism |
| Priority cannot be changed | No Android priority control | Use a compatible other client or implement the control |

[Default creation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L143-L161) [Parent visibility](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/MainViewModel.kt#L22-L26) [Date rule](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/components/Common.kt#L349-L358) [Metric routes](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/screens/DashboardScreen.kt#L85-L113)

## Account handoff and restore incidents

If another account receives old local records, stop additional sync runs and preserve evidence. This is consistent with sign-out's full-snapshot behavior, not proof the server ignored authentication. Identify which installation held the records and which account's next sync registered it. Recovery may require intentional reconciliation on the server and all clients; signing out repeatedly re-arms the snapshot and can repeat the problem. [Disconnect and snapshot](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L50-L58) [Full-state generation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L196-L219)

If launch fails after restore, inspect for encrypted-preference/Keystore errors before touching the database. A credential-key problem is distinct from corrupt task data. The app has no credential-only recovery UI or backup rules, so a maintainer may need to implement a targeted recovery build. Never represent clear app storage as risk-free: it can remove unsynced work and full preserved desktop metadata. See [backup/recovery](12-migrations-backup-and-recovery.md).

## Useful bug report template

```text
App version / version code:
Source commit or artifact provenance, if known:
Device model and Android API:
Operation: local save / sign-in / registration / pull / push / restore
Approximate timestamp and timezone:
Exact visible message, with secrets removed:
Signed in? Registered? Changes to send?:
Expected behavior:
Observed behavior:
Smallest reproduction using disposable data:
Did the issue start after update, restore, or account change?:
Relevant redacted log excerpt:
```

Do not include actual credentials or private datasets in the template. A concise report that identifies the failing layer is more useful than repeated retries followed by a destructive reset.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
