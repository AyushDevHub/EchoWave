# EchoWave release test plan

## Automated checks

Run from the repository root:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease
```

The unit suite covers stream pipeline/range handling, playback routing, search, settings, lyrics, library, and utilities. Lint passes with warnings that need review. A successful debug build does not validate release signing, R8 behavior, or store compliance.

## Physical-device release regression

Use a clean install and a supported Android device. Record device model, Android version, build hash, and test result for each item.

| Scenario | Expected result | Status |
| --- | --- | --- |
| Search: empty, no results, provider error, retry | Clear empty/error/loading states; retry recovers | Not run in this audit |
| Playback: play, pause, next, previous, queue end | UI, notification, and lock-screen state stay synchronized | Not run |
| Seek/range: repeated forward/backward seeks, including near start/end | No recurring 403/range failure; timeline remains accurate | Not run |
| Network: offline, timeout, 403, 404, 500, expired media URL | Bounded retries and user-readable failure/recovery | Not run |
| Background/lifecycle: home, lock, task removal, relaunch | Audio and session controls behave as specified | Not run |
| Bluetooth/headset/media buttons | Controls route to the active queue | Not run |
| Fast repeated track taps | No stale resolver result replaces the latest selection | Not run |
| Library/settings persistence | Favorites, playlists, history, display name, palette survive process restart | Not run |
| Accessibility/display scaling | TalkBack labels, large font, small/large displays remain usable | Not run |
| Clean install and upgrade | First-run and migration/data retention are correct | Not run |
| Release R8 build | Player, stream resolution, lyrics, and persistence survive shrinking | Not run |

## Evidence collected in this workspace

- Debug APK assembled successfully during this review.
- Unit tests pass, including the new settings persistence coverage.
- The latest local report contains 72 unit tests with no failures or errors.
- Release APK and bundle tasks pass with R8 enabled, but outputs are unsigned because release signing is not configured.
- Debug build installed and launched on a connected device for visual UI smoke checks; stream playback and transport regressions were not run.
- Physical-device availability was detected, but no full release regression was executed.
