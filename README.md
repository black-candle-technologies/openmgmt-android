# OpenMGMT for Android

The OpenMGMT project-management app for Android: tasks, projects, scheduling,
and daily planning, synced across devices through the
[OpenMGMT sync server](https://github.com/black-candle-technologies/openmgmt).

## Status

Scaffold. The app is under active development.

## Architecture

- **Sync**: the app speaks the OpenMGMT sync protocol (`omgp/1`) against the
  sync server. Device registration is gated on a Black Candle account:
  the app runs the native OAuth flow (system browser, PKCE S256,
  custom-scheme redirect) against `auth.blackcandletech.com`, then registers
  the device with the access token as its Bearer credential. Subsequent syncs
  use the device token issued at registration. Signing out drops the device
  registration; the next sign-in registers a fresh device and re-sends all
  local data.
- **Storage**: local-first. The on-device database is the source of truth.
  Every change is logged as a sync event (the same entity JSON the desktop
  app uses), and a sync pulls and replays remote events (last write wins in
  server order), then pushes local ones. Nothing is hard-deleted: tasks are
  canceled and projects/organizations archived, as on desktop.
- **Server**: production sync server lives at
  `https://openmgmt.blackcandletech.com`. See the
  [sync protocol docs](https://github.com/black-candle-technologies/openmgmt/blob/main/docs/OPENMGMT_PROTOCOL.md)
  and the [sync manual](https://github.com/black-candle-technologies/openmgmt/blob/main/docs/SYNC_MANUAL.md).

## Building

Prerequisites: Android Studio (Hedgehog or newer), JDK 17, Android SDK 34.

```bash
# generate the Gradle wrapper jar once (not checked in):
gradle wrapper
./gradlew assembleDebug
```

Or just open the project in Android Studio and press Run.

> **Note:** the OAuth redirect `com.openmgmt.android:/oauth2/callback` must be
> registered in the client's metadata document at
> `https://blackcandletech.com/oauth/openmgmt-android.json` before sign-in
> will succeed against `auth.blackcandletech.com`.

## Related repositories

- [openmgmt](https://github.com/black-candle-technologies/openmgmt) — sync
  server, sync protocol, desktop app, MCP server
- [auth](https://github.com/black-candle-technologies/auth) — Black Candle
  auth microservice (OAuth provider)

## License

MIT. See [LICENSE](LICENSE).
