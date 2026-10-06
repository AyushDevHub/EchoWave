# EchoWave pre-release security and release audit

**Review date:** 2026-10-07
**Reviewed artifact:** signed local release APK and AAB; working tree at review time
**Release decision:** **BLOCKED**. This is a source review, not a store or legal approval.

## Findings that block public release

| Severity | Finding | Required before release |
| --- | --- | --- |
| Critical | The app uses undocumented YouTube/YouTube Music InnerTube endpoints and supports background playback. YouTube's [Developer Policies](https://developers.google.com/youtube/terms/developer-policies) prohibit undocumented APIs without express permission and prohibit background audio playback for API Clients (sections III.E.4 and III.E.6). | Treat public distribution as blocked until provider-policy applicability/permission is resolved or the integration is replaced with permitted sources. See [InnerTube notes](INNER_TUBE_NOTES.md). |
| Critical | Music, artwork, lyrics, and provider terms/rights have not been independently cleared for redistribution or public release. | Confirm rights and applicable service terms for every source and asset. |
| High | The latest lint pass has no errors, with 49 warnings and 1 hint remaining. Most warnings concern available dependency updates and deprecations; they still need a reviewed disposition. | Review warnings and current dependency advisories before release; don't use a baseline to conceal findings. |
| High | No dependency vulnerability/SBOM/license scan is configured. | Run a current advisory scan and generate a complete transitive dependency/license inventory. |
| High | `allowBackup` is enabled with sample/default backup rules. Local preferences include display name, music taste terms, and search history; library history/favorites are local Room data. | Decide backup/privacy behavior, configure precise backup exclusions or consent, and align privacy disclosures. |
| High | No published privacy policy or Play Data Safety declaration was verified. | Publish disclosures that match actual provider traffic and SDK behavior; complete Data Safety in Play Console. |
| High | Release signing reads a private per-user properties file; no signed release has been installed or exercised. | Verify signing identity, safely back up the key, and install/test the signed release before any permitted distribution. |

## Checks observed in this checkout

| Area | Result | Evidence / limit |
| --- | --- | --- |
| Source secret scan | **Needs review** | No private-key or common cloud-token pattern was found by the targeted scan. A public InnerTube client key is embedded in source and appears in Git history; it is not a confidential user/server secret, but its ownership and provider terms still need review. No dedicated Gitleaks/TruffleHog binary is installed, so this is not a comprehensive secrets audit. |
| Ignore rules | **Pass, limited** | `local.properties` is ignored. No keystore file was found in ignored workspace status. The scan did not verify remote repositories, CI secrets, or every historical blob. |
| Release signing | **Pass for local artifact** | Release key and properties are outside the checkout under `%USERPROFILE%/.android/`; `apksigner` reports the signing certificate. Back up the keystore and properties securely. |
| Debuggable/release shrink | **Partial pass** | Release merged manifest has no `debuggable=true`; R8/minified APK and AAB tasks pass. APK/AAB signatures were verified; full artifact contents were not exhaustively inspected. |
| Network transport | **Partial** | App endpoints inspected use HTTPS, OkHttp uses normal platform TLS validation, and no cleartext opt-in is present. No live TLS interception, certificate, timeout, offline, or HTTP error testing was performed. |
| URL/token logging | **Partial pass** | Search-query and exception/URL log content was removed in this review. Stream code logs host/range/status metadata. Static scan is not a complete runtime log capture; inspect final release logs on-device. |
| Exported components | **Pass, manifest-level** | Launcher activity is exported; media playback service is not. No other app components are declared in the main manifest. |
| Permissions | **Partial pass** | Manifest requests Internet and foreground media playback only. OS/runtime behavior still needs testing on supported Android versions. |
| Local data | **Partial** | Favorites, history, playlists, settings, taste terms, and recent searches are stored locally. No account system, analytics, or crash SDK appears in the declared app dependencies. Backup is enabled and must be resolved before a privacy claim. |
| Dependency versions | **Needs review** | Android Lint reported multiple newer versions available. Newer does not automatically mean safer; upgrade in a controlled playback regression cycle and review advisories. |
| License/attribution | **Partial** | `LICENSE`, `NOTICE`, and `CREDITS.md` exist and document the Echo-Music donor. Complete dependency/license inventory and asset/music rights remain open. |
| Unit tests | **Pass** | `./gradlew test` completed successfully: 72 tests, 0 failures/errors. No instrumented test task or full device regression was completed in this audit. |
| Debug assembly | **Pass** | `.\gradlew.bat assembleDebug` completed successfully. This is not a release artifact. |
| Lint | **Pass with warnings** | `./gradlew lint` completed with 49 warnings and 1 hint, no errors. Media3 unstable APIs are explicitly opted into through module lint configuration; remaining warnings need review. |
| Current release build | **Pass** | `.\gradlew.bat test lint assembleRelease bundleRelease` completed successfully on 2026-10-07. The APK uses the locally configured release certificate; retain the private key securely. |
| Release APK/AAB tasks | **Pass, signed** | `test lint assembleRelease bundleRelease` completed successfully. APK verification passed with `apksigner`; AAB verification passed with `jarsigner`. The release was not installed or exercised on a clean device. |
| Device playback | **Not verified** | A connected Android device is available, but full stream, seek, retry, background, notification, lock-screen, Bluetooth, and long-session scenarios were not run here. |
| Store readiness | **Not verified** | No Play Console access, listing, internal track, signed bundle, Data Safety submission, or review status was available. |

## Release gate

Do not label this build releasable until the critical rows above are closed. At minimum:

1. Configure and protect release signing; build and inspect a signed AAB/APK.
2. Run current dependency security and license scans; review current provider/API terms and media rights.
3. Decide backup and privacy behavior; publish the policy and complete Data Safety disclosures.
4. Run `assembleRelease`, `bundleRelease`, `test`, and `lint`; resolve all errors and review warnings.
5. Install the signed release on a clean physical device and repeat playback, seeking/range, resolver recovery, lifecycle, notifications, lock screen, Bluetooth, accessibility, and upgrade checks.
6. Inspect artifact contents and logs for secrets, debug endpoints, test data, signed URLs, tokens, and debug-only UI.
7. Complete Play internal testing and store review before production release.

Related records: [Privacy](PRIVACY.md), [Test plan](TEST_PLAN.md), [InnerTube notes](INNER_TUBE_NOTES.md), and root [SECURITY.md](../SECURITY.md).
