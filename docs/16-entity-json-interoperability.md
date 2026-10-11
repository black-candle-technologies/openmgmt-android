# Entity JSON, preserved fields, and desktop interoperability

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Mapping contract

Android models only part of the desktop entity shape. For each accepted remote entity it stores the entire JSON object in `remoteJson`. An outgoing entity begins from a parsed copy of that object, overwrites Android-owned fields, and inserts defaults for selected missing desktop fields. Unknown top-level properties can therefore survive an ordinary Android edit. [Mapping design](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L24-L32) [Base/default helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L83-L96)

If `remoteJson` is absent, malformed, or not a JSON object, the base helper silently starts from an empty map. This fallback prevents a parse failure at that specific step but can discard preserved fields in the next outgoing payload. Some individually accessed preserved fields, such as a nonprimitive slug, can still throw later. The mapper is not a general JSON schema validator.

## Timestamp and slug helpers

Outgoing timestamps use Java Instant from epoch milliseconds and produce UTC text. Incoming timestamps use OffsetDateTime parsing and are reduced to milliseconds. Missing created/updated values use the current clock during import; malformed present values throw. [Timestamp helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L51-L55) [Inbound defaults](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L163-L196)

`slugify` trims/lowercases the name, preserves ASCII a–z and 0–9, compresses other character runs to hyphens after content begins, and trims trailing hyphens. New slugs are `base-sixCharacterIdPrefix`, with base falling back to item and the suffix drawn from the first six alphanumeric characters of the ID. Existing remote slugs are preserved even after renaming an entity. The suffix reduces common-name collisions but is not a mathematical or database uniqueness guarantee. [Slug helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L57-L81)

## Organization field mapping

| JSON field | Outbound behavior | Inbound local representation |
|---|---|---|
| id | Overwrite with local ID | Required primitive content; stored id |
| name | Overwrite local name | Required primitive content; stored name |
| slug | Preserve remote slug, otherwise derive | Preserved only in remoteJson |
| description | Add null if missing; preserve otherwise | Preserved only |
| color | Add null if missing; preserve otherwise | Preserved only |
| icon | Add null if missing; preserve otherwise | Preserved only |
| created_at | Overwrite formatted local createdAt | Parse or current clock if absent |
| updated_at | Overwrite formatted local updatedAt | Parse or current clock if absent |
| archived_at | Overwrite local archive time/null | Parse nullable archivedAt |
| Any other property | Preserve accepted baseline | Preserved only |

[Organization serialization](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L98-L111) [Organization import](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L163-L170)

Renaming changes name but does not regenerate an existing slug. Newly created organizations have no local description/color/icon fields; these are null in the initial standard payload, unless they originated remotely. Archiving through a full snapshot includes archived_at even though a normal delete uses the short archive envelope.

## Project field mapping

| JSON field | Outbound behavior | Inbound local representation |
|---|---|---|
| id | Local ID | Required id |
| organization_id | Require non-null local organizationId | Nullable organizationId |
| name | Local name | Required name |
| slug | Preserve or derive | Preserved only |
| description | Default null only if missing | Preserved only |
| project_type | Default `other` only if missing | Preserved only |
| status | Local status | String or default active |
| priority | Default 3 only if missing | Preserved only; no dedicated project column |
| deadline | Default null if missing | Preserved only |
| repo_url | Default null if missing | Preserved only |
| notes | Default null if missing | Preserved only |
| created_at | Local createdAt | Parsed or current clock |
| updated_at | Local updatedAt | Parsed or current clock |
| archived_at | Local archivedAt/null | Parsed nullable |
| Any other property | Preserve accepted baseline | Preserved only |

[Project serialization](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L114-L131) [Project import](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L172-L181)

A present JSON null is considered present by `default`, so it is not replaced with a default. Android does not validate preserved project priority/type before sending. An inbound project with no organization ID can be stored; a later local save fills a null parent, while an explicit non-null invalid parent remains possible.

## Task field mapping

| JSON field | Outbound behavior | Inbound local representation |
|---|---|---|
| id | Local ID | Required id |
| project_id | Require non-null local projectId | Nullable projectId |
| title | Local title | Required title |
| description | Notes string, or null when blank | Primitive content or empty notes |
| status | Local status | Primitive content or default inbox |
| priority | Local value if 1–5, otherwise 3 | Parse integer or default 3; range not clamped on import |
| due_at | Formatted local dueAt or null | Parsed nullable dueAt |
| scheduled_at | Default null only if absent | Preserved only |
| started_at | Local startedAt or null | Parsed nullable startedAt |
| completed_at | Local completedAt or null | Parsed nullable completedAt |
| estimated_minutes | Default null if absent | Preserved only |
| time_limit_minutes | Default null if absent | Preserved only |
| pinned | Default false if absent | Preserved only |
| blocked_reason | Default null if absent | Preserved only |
| tags | Default empty array if absent | Preserved only |
| created_at | Local createdAt | Parsed or current clock |
| updated_at | Local updatedAt | Parsed or current clock |
| Any other property | Preserve accepted baseline | Preserved only |

[Task serialization](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L134-L153) [Task import](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L183-L196)

Only due_at is exposed as Android Due. Neither scheduled_at nor other unknown scheduling fields drive the Android schedule screen. The client can retain a desktop blocked_reason while allowing status changes without a reason editor. Updating Android status does not explicitly clear that preserved reason. Likewise tags and pinned can survive invisibly.

## Payload wrappers

Normal create/update/transition events use a full object under entity:

```json
{
  "entity": {
    "id": "example-task-id",
    "project_id": "example-project-id",
    "title": "Review the treatment",
    "description": "Check the second-act turn",
    "status": "ready",
    "priority": 3,
    "due_at": null,
    "scheduled_at": null,
    "started_at": null,
    "completed_at": null,
    "estimated_minutes": null,
    "time_limit_minutes": null,
    "pinned": false,
    "blocked_reason": null,
    "tags": [],
    "created_at": "2026-10-11T12:00:00Z",
    "updated_at": "2026-10-11T12:00:00Z"
  }
}
```

Normal archive events use:

```json
{
  "id": "example-task-id",
  "archived_at": "2026-10-11T13:00:00Z"
}
```

These are documentation examples from mapper rules, not live captured data or proof that a particular server accepts the illustrative IDs. Production entities use generated UUID strings by default. [Envelope builders](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L156-L161)

## Import validation and leniency

The `string` helper accepts any non-null JsonPrimitive and returns its content; it does not require a JSON string type. IDs/title/name are asserted non-null by the mapping functions, but empty strings are not explicitly rejected there. A numeric primitive can become string content for one of these fields. Missing optional timestamps become null or default current time, while malformed timestamp text throws. Nested objects where primitives are expected are treated as absent by this helper. [Import helpers](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L93-L96) [Mapping functions](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncPayloads.kt#L163-L196)

Before calling the mapper, the repository checks that the inner entity ID equals the envelope entity_id, validates supported operation strings, and selects a supported entity type. Unsupported or malformed events are caught per event, logged, and permanently marked processed under current replay semantics. This permits sync progress but creates a compatibility blind spot when the server adds new entity types. [Repository validation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L259-L289)

## Round-trip guarantees and non-guarantees

| Scenario | Expected source behavior |
|---|---|
| Accepted remote task has unknown field; Android edits title later | Unknown field retained from remoteJson |
| Accepted remote slug; Android renames project | Existing slug retained |
| No baseline remoteJson | Mapper supplies its known defaults only |
| Malformed baseline JSON | Empty map fallback; preserved fields can disappear |
| Concurrent remote event while local entity has pending work | Event skipped, so newer unknown fields are not merged |
| Remote invalid priority such as 99 | Local field can store 99; a later outbound task payload writes 3 |
| Remote timestamp has nanosecond precision | Local modeled timestamp loses submillisecond precision |
| Remote entity missing parent | Nullable parent can import; later local save may file under default |
| Android local archive later snapshotted | Full updated payload carries archive/canceled state |

These outcomes do not establish binary compatibility with every desktop release. Add contract fixtures pinned to the server/desktop revision before promising support for new fields, statuses, operations, or permissions.

## Maintainer extension checklist

A new Android-editable field needs a local model decision, migration if stored, inbound parse, outbound overwrite/default policy, editor state (including the task Saver where relevant), repository mutation behavior, and bidirectional fixture tests. For a field retained only in remoteJson, make sure local operations do not accidentally override it or create contradictory derived state. Version protocol behavior intentionally if clients must reject or quarantine new events instead of silently consuming them.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
