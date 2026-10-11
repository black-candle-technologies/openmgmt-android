# OAuth, PKCE, and credential lifecycle reference

> Source snapshot: `e53308af3921f454be1c661691b1fe546cb8f679` · reviewed 2026-10-11 UTC. See [scope and evidence](README.md#scope-and-evidence).

## Authentication design

The Android client is a public native OAuth client. It performs Dynamic Client Registration, opens authorization in a system-browser Custom Tab, uses S256 PKCE and random state, receives a custom-scheme callback, and exchanges the authorization code with a form POST. It never configures a client secret. Its OAuth access token is used to register with the sync service; ongoing event synchronization uses a distinct device credential. [Auth class contract](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L34-L50) [Sync registration boundary](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L73-L96)

| Configuration | Default |
|---|---|
| Issuer | `https://auth.blackcandletech.com` |
| Redirect URI | `com.openmgmt.android:/oauth2/callback` |
| Requested scope | `identity` |
| Dynamic registration | `{issuer}/oauth/register` |
| Authorization | `{issuer}/oauth/authorize` |
| Token exchange/refresh | `{issuer}/oauth/token` |
| OAuth HTTP connect/read timeouts | 15 seconds / 30 seconds |
| Browser wait | 5 minutes |
| Refresh leeway | 5 minutes before locally calculated expiry |

The issuer has trailing slashes removed before endpoint concatenation. There is no discovery-document fetch, audience/resource parameter, ID-token validation, or userinfo lookup. [Config](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/OAuthConfig.kt#L12-L17) [Timeouts](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L31-L32) [HTTP client](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L56-L59)

## Dynamic Client Registration

`ensureClientId` first returns the saved `dcr_client_id` if present. Otherwise it posts this JSON shape:

```json
{
  "client_name": "OpenMGMT Android",
  "redirect_uris": ["com.openmgmt.android:/oauth2/callback"],
  "token_endpoint_auth_method": "none",
  "grant_types": ["authorization_code", "refresh_token"],
  "response_types": ["code"],
  "scope": "identity"
}
```

The request is unauthenticated in this implementation. A non-2xx response produces `Client registration failed: HTTP N`. A success body must include nonempty `client_id` beginning `bct_`; other response properties are ignored. The ID is persisted and reused across sign-outs. The client does not check a registration expiry, retrieve/update metadata, or store a registration-management token. [DCR code](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L203-L229)

The README's note about registering a URL metadata document at `https://blackcandletech.com/oauth/openmgmt-android.json` is inconsistent with this code path. Successful deployment needs the auth service's DCR policy to accept the native redirect, public auth method, grants, and scope. Editing that URL document alone does not configure the implementation being reviewed. [README metadata note](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/README.md#L43-L46)

## Authorization request and persisted state

PKCE generates 32 secure-random bytes for the verifier, encoded base64url without padding into 43 characters. S256 hashes the verifier's US-ASCII bytes with SHA-256 and applies the same encoding. OAuth state is 16 random bytes encoded into 22 characters. Helpers use `java.util.Base64`, which makes them JVM-testable and aligns with the declared minimum API 26. [PKCE implementation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/Pkce.kt#L8-L31)

`beginSignIn` saves pending verifier and state, then builds these query parameters: `response_type=code`, the registered client ID, redirect URI, scope, state, code challenge, and `code_challenge_method=S256`. It installs an in-memory `CompletableDeferred`, opens the Custom Tab with NEW_TASK, and awaits the deferred for at most five minutes. The `finally` block removes that waiter if it still owns it. [Authorization construction](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L79-L115)

The five-minute timeout applies to waiting, not a stored expiry on pending values. The timeout path does not clear pending verifier/state. A later callback may still use them until a new sign-in, sign-out, or callback consumes them. Process death discards the waiter but leaves persisted pending values available; callback handling can save tokens without an active waiter.

## Callback dispatch and validation

The manifest declares a browsable VIEW filter with scheme `com.openmgmt.android` and path `/oauth2/callback`; the activity is exported and singleTask. Runtime dispatch accepts that scheme/path pair and forwards the URI to AuthManager in a lifecycle coroutine. The predicate is not a full comparison to `OAuthConfig.redirectUri`: host and port are not checked. [Manifest callback](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/AndroidManifest.xml#L14-L34) [Runtime gate](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/MainActivity.kt#L122-L138)

`handleRedirect` returns false if pending verifier/state are absent. Otherwise it removes them immediately, before checking the callback. It reads `error`, `code`, and `state`. An error causes an authorization-failed exception; missing code or unequal state causes Invalid OAuth callback; a valid pair proceeds to token exchange. Success completes the waiter and saves tokens; failure completes the waiter exceptionally and rethrows to the activity logger. [Callback handling](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L125-L163)

**Security consequence, inferred from source:** a matching deep link with incorrect state can consume the pending attempt, causing denial of the legitimate callback. State and PKCE still protect the successful exchange path; the observation is a flow-interruption risk, not proof of token theft. The callback-removal logic also lacks a single lock covering read/consume/exchange, so concurrent entry paths deserve a targeted regression test. Recommendations are in [security](17-security-and-hardening.md).

## Code exchange

The token request is `application/x-www-form-urlencoded` with:

| Field | Value |
|---|---|
| grant_type | authorization_code |
| code | Callback authorization code |
| redirect_uri | Configured native URI |
| client_id | Stored/ensured DCR client ID |
| code_verifier | Persisted verifier |

No Authorization header or secret is added. Any non-2xx response produces a generic HTTP-code exception. A 401 additionally deletes the DCR client ID so the next sign-in re-registers; other statuses retain it. The current failing attempt is not transparently restarted. [Exchange implementation](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L265-L288)

A successful JSON response must contain nonempty access_token. refresh_token is optional; empty becomes absent. expires_in is read as a Long and defaults to 3600. Token type, scope, issuer, subject, and audience fields are not inspected. Local expiry becomes `System.currentTimeMillis() + expires_in * 1000`; no explicit range validation guards that arithmetic. [Parsing and storage](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L239-L263)

## Access token refresh

`validAccessToken` holds the AuthManager mutex. If a nonempty token exists and now is before stored expiry minus five minutes, it returns the token. Otherwise it requires refresh_token and client_id, then posts grant_type refresh_token, refresh_token, and client_id to the token endpoint. Holding the mutex serializes refreshes within this AuthManager instance, important for a rotating-token service. [Refresh decision](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L171-L185) [Refresh request](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L290-L317)

On success the new access token and expiry replace the previous values. If the server omits refresh_token, storage removes the existing one; the client does not retain it automatically. The server therefore needs to return a replacement refresh token for continued refresh support under this client's expectations. Network exceptions occurring before an HTTP response do not enter the non-success branch. However **every non-2xx response**, including transient server errors, clears access token, refresh token, and expiry and throws the generic session-expired message. [Token storage](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L239-L252) [Refresh failure](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L304-L316)

Refresh is demanded by device registration or re-registration, not by every sync. An existing device token can keep synchronization operating without exercising OAuth expiry. `isSignedIn` only checks preference presence, so a long-expired access token can continue to be labeled signed in until some path demands a valid token or clears it.

## Storage inventory

Both secure preference stores use MasterKey AES256_GCM, AES256_SIV for preference keys, and AES256_GCM for values. [Auth storage](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L61-L72) [Device storage](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt#L15-L25)

| Store | Keys | Lifecycle |
|---|---|---|
| `openmgmt_auth` | dcr_client_id | Created by DCR; retained on sign-out; removed by code-exchange HTTP 401 |
| `openmgmt_auth` | access_token, refresh_token, token_expires_at | Replaced by exchange/refresh; cleared on sign-out and non-2xx refresh |
| `openmgmt_auth` | pending_verifier, pending_state | Created before browser launch; consumed by any processed pending callback; cleared on sign-out |
| `openmgmt_sync_secure` | device_id | Lazy UUID; replaced on disconnect |
| `openmgmt_sync_secure` | device_token, account_id, user_id | Saved on sync registration; removed on reset |
| Legacy `openmgmt_sync` | device_id, device_token | Copied lazily into secure store, then legacy preferences cleared |

The legacy migration does not copy account/user IDs and does not check whether newer secure keys already exist before writing legacy values. A residual legacy file can therefore overwrite corresponding secure keys on initialization. There is no migration-version marker. [Legacy migration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncCredentials.kt#L61-L77)

## Sign-out and revocation boundaries

`AuthManager.signOut` removes account tokens, expiry, pending flow values, and drops the waiter reference. It does not complete/cancel an existing deferred explicitly and does not revoke anything remotely. The UI prevents ordinary sign-out during active sign-in/sync, but these manager methods have no general session-generation guard against in-flight completion. `SyncViewModel` additionally calls disconnect to rotate the local device ID and mark a full snapshot pending for materialization during a later sync. [Auth sign-out](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/auth/AuthManager.kt#L187-L198) [UI sign-out](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/ui/SyncViewModel.kt#L96-L111)

The Android source offers no PAT entry point, no API-key field, no browser-cookie management, no account subject display, and no logout endpoint call. Do not confuse the OAuth access token, rotating refresh token, public DCR client ID, and sync device token in runbooks. Their issuers, uses, and lifetimes differ, and the client does not prove server-side revocation by deleting local values.


## Compatibility with the reviewed auth repository

The companion auth wiki pins server source to `f22dfb2879497c8999cc722e13d9f39c0dd4eb18`. Comparing that source with this Android revision establishes a compatible intended message contract for DCR, native authorization, and refresh. It does not establish which commit is running at the configured host or that a live round trip succeeded. See [the auth native-client guide](../auth/11-dynamic-registration-and-native-apps.md) and [credential lifecycles](../auth/14-token-session-and-credential-lifecycles.md).

| Boundary | Android behavior | Matching auth implementation |
|---|---|---|
| Public registration | Sends no secret, requests auth method none and both code/refresh grants | Accepts that profile, stores eligible grants, and creates a public `bct_` ID |
| Native callback | Uses `com.openmgmt.android:/oauth2/callback` | Dotted custom scheme with this path is accepted; code exchange requires the exact authorized URI |
| Browser return | Waits for the custom-scheme intent | Native approval produces an HTML page with an automatic return attempt and **Return to app** link |
| Code proof | Sends the original PKCE verifier | Computes S256 and compares it with the stored challenge |
| Initial token pair | Saves access, optional refresh, and relative expiry | Eligible DCR code grant returns access plus refresh; access lifetime is one hour |
| Renewal | Sends current refresh value and DCR ID, then replaces saved tokens | Consumes the old refresh value and returns a new token pair with the same scope |

Sources: [Server DCR handler and accepted profile](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth_dcr.go#L97-L228) [Native URI validation](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth_dcr.go#L246-L270) [Exact code binding and PKCE](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth.go#L486-L565) [Native return](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth.go#L881-L913) [Pair creation and refresh](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth_refresh.go#L49-L179).

The DCR response has six fields: client_id, client_name, redirect_uris, token_endpoint_auth_method, grant_types, and response_types. It contains no issued-at timestamp. Android reads only client_id, so extra response metadata is not a prerequisite to this client flow. [Server response struct](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth_dcr.go#L88-L95) [Response construction](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth_dcr.go#L141-L148)

These credential types remain separate. The auth service's website session authorizes session-protected account operations. Its OAuth access token and PAT are both accepted at `/oauth/userinfo`; its refresh token is accepted by the eligible refresh grant. Android implements the OAuth authorization flow and supplies its resulting access token for sync device registration. It has no PAT entry field or account-session API integration. The sync service then issues its own device credential. The existence of PAT support in auth does not demonstrate that the unreviewed sync server accepts a PAT in place of an OAuth access token for device registration. [Auth userinfo OAuth/PAT resolution](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/oauth.go#L576-L652) [Session-only PAT management](https://github.com/black-candle-technologies/auth/blob/f22dfb2879497c8999cc722e13d9f39c0dd4eb18/internal/api/personal_tokens.go#L62-L81) [Android registration](https://github.com/black-candle-technologies/openmgmt-android/blob/e53308af3921f454be1c661691b1fe546cb8f679/app/src/main/java/com/openmgmt/android/sync/SyncManager.kt#L73-L96)

---

[Wiki home](README.md) · [Source coverage](24-source-coverage.md) · [Verification limits](20-testing-and-verification.md)
