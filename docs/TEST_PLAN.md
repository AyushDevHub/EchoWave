# Test Plan

This plan separates automated checks from device verification. A green build does not establish provider permission, content rights, app-store compliance, or complete playback reliability.

## Automated checks

Run from the repository root on Windows:

```powershell
.\gradlew.bat test lint assembleDebug
```

For a release candidate, also run:

```powershell
.\gradlew.bat assembleRelease bundleRelease
```

The v1.0.0 build was produced with release signing configured locally. The unit suite reported 72 tests with no failures/errors. Lint completed without errors and reported 49 warnings and 1 hint; those findings still need review. The signed APK and AAB signatures were verified. These checks did not exercise playback on a clean device.

## Device regression matrix

Run on at least one clean physical device using the signed release APK. Record device, Android version, APK SHA-256, date, and outcome. Do not write private user data into the report.

| Area | Scenario | Expected outcome | v1.0.0 status |
| --- | --- | --- | --- |
| Install/upgrade | Clean install, open, upgrade over previous signed build | Launches; data migration and signing identity behave as intended | Not verified |
| Search | Empty input, no result, provider error, retry | Clear validation, empty/error states, recoverable retry | Not verified |
| Playback | Play, pause, next, previous, end of queue | UI, queue, notification, and lock-screen controls remain consistent | Not verified |
| Seeking | Repeated forward/back seek, including near start/end | Position remains accurate; no repeatable range/403 failure | Not verified |
| Network recovery | Offline, timeout, 403, 404, 500, expired URL | Bounded retries and understandable failure state | Not verified |
| Lifecycle | Background, lock, return to app, task removal, process recreation | Expected session and service behavior; no stale playback state | Not verified |
| Hardware controls | Headset, Bluetooth, notification, lock screen | Commands reach the current queue correctly | Not verified |
| Rapid selection | Tap several tracks quickly | Latest selection wins; no stale resolver result replaces it | Not verified |
| Library/settings | Favorites, playlists, history, name, palette, recent search | State persists and clear/remove actions work | Not verified |
| UI/accessibility | TalkBack, large font, smaller display, rotation if supported | Controls remain discoverable and layout usable | Not verified |
| R8/release | Exercise playback, lyrics, persistence in minified signed build | No missing-code or serialization failures | Not verified |

## Evidence recorded for v1.0.0

- `test lint assembleRelease bundleRelease` succeeded in the local development environment.
- `apksigner` verified the release APK; `jarsigner` verified the release AAB.
- A debug build was installed/launched for a visual UI smoke check. Full stream playback and the device regression matrix above were not run.
- The GitHub `Publish EchoWave APK` workflow completed successfully and published the APK/checksum assets for tag `v1.0.0`.
- Lint warnings, provider-term questions, privacy/backup decisions, complete dependency inventory, and device playback checks remain follow-up work.

## Release evidence to retain

Keep the source commit and tag, test/lint reports, signed artifact checksums, signer certificate fingerprint, device test record, dependency/SBOM report, and release notes together. Never store the private signing key or its password in the repository or CI logs.
