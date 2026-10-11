# OMGP wire protocol and HTTP client reference

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Scope

This page describes the Android client's actual `omgp/1` message model and transport. It does not certify that a particular running server implements every expectation. The Kotlin model comments point to `openmgmt-protocol` in the desktop/server repository; that relationship must be verified against a pinned compatible server revision for deployment. [Protocol declaration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L7-L11)

`OmgpClient` appends a fixed path to `baseUrl.trimEnd('/')`, sends JSON with `Content-Type: application/json`, and decodes the successful response with Kotlin serialization. Its JSON configuration ignores unknown response keys and encodes defaults. Connect and read timeouts default to 30 seconds. There is no custom certificate pinner, proxy config, response-byte limit, logging interceptor, explicit whole-call timeout, or application retry/backoff policy in this builder. [Transport](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt#L27-L61)

| Endpoint | Method | Credential placement | Model pair |
|---|---|---|---|
| `/omgp/v1/hello` | POST | None | HelloRequest / HelloResponse |
| `/omgp/v1/devices/register` | POST | OAuth access token in Authorization Bearer | RegisterRequest / RegisterResponse |
| `/omgp/v1/sync/pull` | POST | Device credential in JSON auth | PullRequest / PullResponse |
| `/omgp/v1/sync/push` | POST | Device credential in JSON auth | PushRequest / PushResponse |

[Endpoint implementations](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt#L63-L90)

## Error envelope and HTTP semantics

`ProtocolError` has required string code and message, plus retryable Boolean default false. A response-level error with code `unauthorized` becomes `UnauthorizedException`; every other code becomes `ProtocolException(code, message)`. The retryable flag is parsed but not used to schedule retries. [Error model](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L13-L18) [Error dispatch](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt#L92-L103)

Any non-2xx HTTP status becomes generic `SyncException("Server returned HTTP N")` before typed error decoding. Thus **HTTP 401 is not the same as a 2xx response whose protocol error code is unauthorized**: only the latter reaches the manager's once-per-run re-registration branch. A malformed JSON body or missing required model property fails deserialization rather than becoming a ProtocolException. Servers must return a shape compatible with the chosen response model, including required fields, even when using the optional error member. [HTTP status handling](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt#L54-L60) [Unauthorized recovery](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L98-L106)

## Hello

| HelloRequest field | Type | Default / manager value |
|---|---|---|
| protocol_version | string | `omgp/1` |
| client_name | string | `OpenMGMT Android` |
| client_version | nullable string | null in model; manager supplies BuildConfig.VERSION_NAME |
| device_id | nullable string | null in model; manager supplies current installation ID |

| HelloResponse field | Type | Default |
|---|---|---|
| protocol_version | string | Required |
| server_name | string | Empty |
| server_version | nullable string | null |
| compatible | Boolean | Required |
| error | nullable ProtocolError | null |

The client checks the error, then `compatible`. False raises “Server does not support omgp/1.” It does not independently compare the returned protocol_version string or display server name/version. [Hello models](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L20-L35) [Hello validation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt#L63-L68)

## Device registration

| RegisterRequest field | Type | Behavior |
|---|---|---|
| protocol_version | string | Default omgp/1 |
| device_id | string | Required current device UUID |
| device_name | string | Required; manager uses Android Build.MODEL or Android device |
| previous_device_token | nullable string | null for initial registration; old token for recovery |
| user_hint | nullable string | Existing stored user ID if available |

| RegisterResponse field | Type | Default / requirement |
|---|---|---|
| accepted | Boolean | Required; must be true |
| account_id | nullable string | null allowed |
| user_id | nullable string | null allowed |
| device_token | nullable string | Must be nonempty after error check |
| error | nullable ProtocolError | null |

The client stores returned account/user IDs exactly as optional values. No local check requires them when a device token is accepted. `previous_device_token` is proof supplied for re-registration, not an OAuth refresh token. `user_hint` is not user-entered account selection. [Registration models](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L37-L54) [Acceptance check](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpClient.kt#L70-L78) [Manager registration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L73-L88)

Example request shape, using placeholders rather than a captured request:

```http
POST /omgp/v1/devices/register
Authorization: Bearer <oauth-access-token>
Content-Type: application/json
```

```json
{
  "protocol_version": "omgp/1",
  "device_id": "example-device-id",
  "device_name": "Example Android device",
  "previous_device_token": null,
  "user_hint": null
}
```

## Shared auth object

`AuthContext` has nullable account_id, nullable user_id, required device_id, and nullable device_token. None has a Kotlin constructor default, so the constructor caller supplies every property even when null. The manager reads them from secure sync preferences for each request. There is no bearer header on pull/push in this client. [Auth model](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L56-L62) [Auth construction](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L91-L96)

```json
{
  "account_id": "example-account-id",
  "user_id": "example-user-id",
  "device_id": "example-device-id",
  "device_token": "<device-token>"
}
```

Servers, reverse proxies, and diagnostics must treat request bodies as credential-bearing for these endpoints. Header-only token redaction will not cover the device token.

## Event envelope

| JSON field | Type | Default / interpretation |
|---|---|---|
| event_id | string | Required network event UUID |
| device_id | string | Required producing device ID |
| actor_user_id | nullable string | null; outgoing manager leaves unset |
| target_user_id | nullable string | null; outgoing manager leaves unset |
| workspace_id | nullable string | null; outgoing manager leaves unset |
| sequence | Long integer | Required; Android local outbox sequence |
| entity_type | string | Required; current writers use organization/project/task |
| entity_id | string | Required affected entity ID |
| operation | string | Required; created/updated/transitioned/archived |
| payload_json | JSON object | Required; object, not a string containing JSON |
| created_at | string | Required timestamp text |
| synced_at | nullable string | null default |

[Event model](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L64-L78) [Outgoing event construction](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L131-L141)

In Room, `payloadJson` is stored as a string; at push time it is parsed back into a JsonObject. Local event creation time, rather than entity updatedAt, becomes the envelope created_at. The inner entity carries its own timestamps. Actor/target/workspace fields are modeled for transport compatibility but not used for local authorization or entity partitioning.

## Pull

PullRequest contains protocol_version default omgp/1, required auth, required-but-nullable after_checkpoint, and optional nullable limit default null. The manager supplies its current stored checkpoint and limit 500. PullResponse defaults events to empty, server_checkpoint to empty string, has_more to false, and error to null. [Pull models](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L102-L116)

```json
{
  "protocol_version": "omgp/1",
  "auth": {
    "account_id": "example-account-id",
    "user_id": "example-user-id",
    "device_id": "example-device-id",
    "device_token": "<device-token>"
  },
  "after_checkpoint": null,
  "limit": 500
}
```

A blank returned checkpoint is not saved. The manager breaks on either has_more false or an empty event list. It permits at most 200 pages per run, but does not validate that the server obeyed the requested page size or that checkpoints advanced. [Pull loop](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L115-L124) [Checkpoint persistence](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/data/Repository.kt#L253-L255)

## Push

PushRequest contains protocol_version default omgp/1, required auth, required-but-nullable base_checkpoint, and required list events. PushResponse has accepted_event_ids default empty, rejected_events default empty, server_checkpoint default empty, and error default null. Each rejected event carries required event_id and ProtocolError. [Push models](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/OmgpModels.kt#L80-L100)

An entity mutation payload is `{"entity": {...}}`. An archive payload is `{"id": "...", "archived_at": "..."}`. Those shapes are detailed in [entity mapping](16-entity-json-interoperability.md).

Acceptance is authoritative only for returned IDs that belong to the sent batch; duplicates are removed before marking synced. Unknown accepted IDs do not clear unrelated local rows. Rejected count is taken directly from the response list and is not filtered against the sent set. Rejected or unacknowledged events remain pending. The push loop stops when not every event in the batch was accepted. The response's server_checkpoint is not adopted as the local pull checkpoint. [Push handling](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L128-L153)

## Compatibility requirements for server maintainers

A compatible deployment must support the public native DCR/OAuth contract, OAuth-authorized device registration, device-token auth objects, optional account/user IDs as modeled, all three entity types and four operations, full-entity update payloads, archive payloads, event-ID/sequence retry semantics, and pull checkpoints. Android itself only enforces a subset of those checks. Tests should distinguish protocol unauthorized from HTTP unauthorized and validate error response shapes, pagination limits, oversized payloads, and partial acceptance.

No live request samples are included in this wiki. Examples are shape illustrations derived from source. Do not paste real tokens into documentation, issue reports, or reusable curl commands.

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
