# OpenMGMT for Android

The OpenMGMT project-management app for Android: tasks, projects, scheduling,
and daily planning, synced across devices through the
[OpenMGMT sync server](https://github.com/black-candle-technologies/openmgmt).

## Status

Scaffold. The app is under active development.

## Architecture

- **Sync**: the app speaks the OpenMGMT sync protocol (`omgp/1`) against the
  sync server. Device registration is gated on a Black Candle account:
  the app runs the native OAuth flow (system browser, PKCE S256, loopback
  callback) against `auth.blackcandletech.com`, then registers the device
  with the access token as a Bearer <redacted> Subsequent syncs use the device
  token issued at registration.
- **Storage**: local-first. The on-device database is the source of truth;
  sync pushes and pulls event batches.
- **Server**: production sync server lives at
  `https://openmgmt.blackcandletech.com`. See the
  [sync protocol docs](https://github.com/black-candle-technologies/openmgmt/blob/main/docs/OPENMGMT_PROTOCOL.md)
  and the [sync manual](https://github.com/black-candle-technologies/openmgmt/blob/main/docs/SYNC_MANUAL.md).

## Building

Prerequisites: Android Studio (Hedgehog or newer), JDK 17, Android SDK 34.

```bash
# open in Android Studio, or from the command line:
./gradlew assembleDebug
```

## Related repositories

- [openmgmt](https://github.com/black-candle-technologies/openmgmt) — sync
  server, sync protocol, desktop app, MCP server
- [auth](https://github.com/black-candle-technologies/auth) — Black Candle
  auth microservice (OAuth provider)

## License

MIT. See [LICENSE](LICENSE).
