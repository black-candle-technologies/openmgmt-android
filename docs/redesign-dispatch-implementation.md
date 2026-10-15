# Redesign / Dispatch / updater implementation

Branch: `feat/redesign-dispatch`. App version remains 0.3.3 / 7.

## Implemented

- Light/dark palette, bottom navigation (More retains the drawer), existing sync pill and shared FAB.
- Four dashboard metrics, maximum three attention tasks; exclusive Daily Ops sections and collapsed done-today; quiet task/project filters; date-grouped schedule with NOW and collapsed undated work.
- Pure urgency approximation with deterministic `now` and timezone inputs, exclusive buckets, counts/filtering, grouped cards, and unit tests. It is **not** the desktop ranking engine. Focus sorts priority first; Balanced/Deadline currently sort deadline then priority.
- Visible three-bar priorities and P1–P5 segmented editor; next-week shortcut and delete overflow. Existing complete/Undo behavior retained.
- Count-led project/organization cards and detail sheets. Desktop description/deadline come from retained sync JSON; local projects do not invent them. Repository owner/name/branch can be bound or cleared in project editing.
- Room 3→4 migration. Agent metadata and repository binding are local-only extensions, preserved across sync replay and stale task-editor saves. They are included in JSON export, but not sent through the existing OMGP schema.
- Dispatch HTTPS configuration, existing OAuth bearer acquisition, all seven requested endpoints, default-encoding JSON, draft/session sheets, sign-in/repository/offline states, model selection, prompt editing/regeneration, follow-up/stop/PR actions, 15-second open-session polling and resume refresh. A task lookup reconciles potentially lost dispatch responses before another submission.
- Settings export through Android's document picker (including archived data, excluding credentials), persisted ranking choice, Advanced Dispatch URL, manual updater entry.
- Updater: schema-1 manifest selection, universal/single-file fallback, long version-code comparison, 3-second startup delay, completed-scan one-hour throttle, manual bypass, prompt, non-cancelable download progress, manifest SHA-256 plus optional GitHub asset digest, APK package/version identity checks, signer-pin hook, unknown-sources settings and narrowly scoped FileProvider installation.
- Restored official Gradle 8.7 wrapper; wrapper JAR checked against Gradle's published SHA-256, distribution pinned by SHA-256. CI invokes `./gradlew`.

## OAuth branch evaluation

Read the upstream OAuthConfig and bearer-client files through the GitHub tools and compared the fetched fix branches with current main. Main already uses `com.openmgmt.android:/oauth2/callback` consistently and sends registration's OAuth token as `Authorization: Bearer …`. No additional fix was necessary. Wholesale cherry-picks would regress newer protocol error handling, encodeDefaults, and JVM-testable PKCE. OAuth/PKCE were not rewritten.

## Signing / updater caveat

`UPDATE_SIGNER_SHA256` is deliberately empty with a TODO. Repository CI has no stable production signing configuration (release builds are unsigned; debug builds use generated debug keys), so no production certificate pin could be established from it. Only the certificate pin check skips, with a warning log, when this constant is empty. Manifest checksum, asset digest when supplied, package ID, and version checks still fail closed. Production release owners must supply a stable signer pin and publish `release.json`; releases without that asset are ignored. Device installation has not been exercised here.

## Remaining validation / limitations

- Dispatch host DNS did not resolve from this runner. Live service integration remains unverified. The brief specified endpoints but not request/response field schemas: this client currently sends `task_id`, `title`, `notes`, `repo_owner`, `repo_name`, `repo_branch`, `prompt`, `model`; expects `id/state/pr_url`, `prompt`, model `id/provider/model`, and event `message/type`. Follow-ups send `message`. Confirm these against the service before shipping; there is no claim of a validated server contract.
- AuthManager currently exposes sign-in state, not account email/user-info. Sync retains working sign-in/sign-out but displays “Signed in” rather than inventing an email or adding an undocumented identity endpoint.
- Full desktop ranking parity, editable desktop-only project description/deadline and tag editing are not implemented (tags were not exposed in the existing editor).
- No emulator/device or local JDK/Android SDK is available in this job. CI supplies Android build, unit test, lint and release validation; manual light/dark/accessibility, OAuth browser return, migration-on-device, Dispatch and installer tests remain required.
- Dispatch calls use finite OkHttp timeouts on IO; local task use is independent. Session transport failure retains local task data. No background agent polling occurs after the session is closed (apart from explicit page-resume refresh).
