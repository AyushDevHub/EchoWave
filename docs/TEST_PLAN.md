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

The v1.0.0 build was produced with release signing configured locally. The unit suite reported 72 tests with no failures/errors. Lint completed without errors and reported 49 warnings and 1 hint at that time; those findings still need review. The signed APK and AAB signatures were verified. These checks did not exercise playback on a clean device.

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

## v1.0.1 candidate device check (2026-10-10)

Device: Motorola Moto G96 5G (`cuscoi_g25`), Android 16 (API 36). Installed the current working-tree debug build over the existing app data before the version metadata was bumped to 1.0.1; the tested app code matches the candidate code.

| Area | Result |
| --- | --- |
| First track lyrics | “Saree Ke Fall Sa” displayed timed lyrics. The highlighted line changed as playback advanced. Tapping the lyric preview left the same track playing; it did not advance the queue. |
| Track change | Started “Lungi Dance (From \"Chennai Express\")”. The player changed title and artwork and did not show the previous track's lyric text while loading. |
| New track lyrics | LyricsPlus lookup returned no candidates and timed out; the UI ended at “Lyrics unavailable” with Retry. Playback continued on the new song. This confirms stale lyrics are cleared, but does not verify synced lyrics for this title. |
| Release behavior | Not verified. This was a debug build; a signed, minified release still needs device testing. |

The phone also reported an unrelated Bluetooth media session in `ERROR` state (“Bluetooth audio disconnected”); EchoWave's own media session reported `PLAYING` with the expected track during the checks above.

Automated checks for the 1.0.1 candidate source completed on 2026-10-10: `test lint assembleRelease bundleRelease` succeeded. Lint reported 49 warnings and 2 hints with no errors; review the generated `app/build/reports/lint-results-debug.html` before treating lint as clean. The signed release APK and AAB were signature-verified locally. Release-mode on-device behavior remains unverified.

The current source checkout was rechecked on 2026-10-10 with `test lint assembleDebug`. The forced `test --rerun-tasks` execution passed 159 tests across 31 test suites with no failures or errors. Lint completed with 0 errors, 49 warnings, and 2 hints; the warnings remain to be reviewed. The build emitted Kotlin/JDK target fallback warnings (the installed JDK target is newer than Kotlin's supported target); these are build-toolchain warnings, not proof that runtime behavior is correct. The debug APK installed on the connected Moto G96 5G and its launcher activity came to the foreground. This was a launch smoke check only; it does not replace the release-mode device regression matrix or verify network playback.

After removing the unused MP3 files, `test lint assembleRelease` was rerun on 2026-10-10 and succeeded. The fresh unit run again passed 159 tests in 31 suites. Lint reported 0 errors, 46 warnings, and 2 hints. `app-release.apk` verified with `apksigner`; its certificate SHA-256 matches the v1.0.0 APK certificate (`08584910f251a6edab224f11f765868e3a4eb92ffb03a3b47894fcb10dd35374`). The release APK archive contains none of the three removed MP3 resources. APK SHA-256: `F388B52ADA3E1D7DAEE0A49710BDCCD290509CB019846A5650D56A193FD27C20`. This release build has not been installed on a device or published; the provider authorization gate remains open.

After adding the GitHub release update chip, `test lint assembleDebug assembleRelease` succeeded on 2026-10-10. The fresh unit run passed 159 tests in 31 suites; lint reported 0 errors, 46 warnings, and 2 hints. The signed release APK verifies with the same certificate as v1.0.0, excludes the removed MP3 resources, and has SHA-256 `89DC8359D3B83DD048E57E29DD24D28C76FC3A6A57E128AB1E20DC5A27346075`. The debug build installed and launched on the Moto G96 5G without an EchoWave fatal exception in the captured logs. At test time GitHub's latest release was v1.0.0 while this candidate reports 1.0.1, so no update chip should be offered; an actual newer-release chip tap was not exercised on-device. Release-mode device regression remains unverified.

## Evidence recorded for v1.0.0

- `test lint assembleRelease bundleRelease` succeeded in the local development environment.
- `apksigner` verified the release APK; `jarsigner` verified the release AAB.
- A debug build was installed/launched for a visual UI smoke check. Full stream playback and the device regression matrix above were not run.
- The GitHub `Publish EchoWave APK` workflow completed successfully and published the APK/checksum assets for tag `v1.0.0`.
- Lint warnings, provider-term questions, privacy/backup decisions, complete dependency inventory, and device playback checks remain follow-up work.

## Release evidence to retain

Keep the source commit and tag, test/lint reports, signed artifact checksums, signer certificate fingerprint, device test record, dependency/SBOM report, and release notes together. Never store the private signing key or its password in the repository or CI logs.
