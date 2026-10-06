# EchoWave v1.0.0 Security and Release Review

**Review date:** 2026-10-07
**Artifact:** signed APK and AAB built locally for tag `v1.0.0`
**Publication:** GitHub Release `v1.0.0` published; APK and SHA-256 asset confirmed through GitHub's release API.
**Scope:** source/configuration review and recorded build evidence. This is not a penetration test, legal opinion, privacy certification, or app-store approval.

## Open risks

| Priority | Finding | Follow-up |
| --- | --- | --- |
| Critical | The app uses undocumented YouTube/YouTube Music InnerTube endpoints and supports audio extraction/background playback. Relevant official terms/policies restrict automated access, undocumented APIs, audio separation, and background playback depending on scope and applicability. No written authorization is documented. | Obtain written permission or replace the integration with a permitted source before further distribution. See [InnerTube notes](INNER_TUBE_NOTES.md) and [risk register](RISK_REGISTER.md). |
| Critical | Rights/permission for remotely supplied music and lyrics, metadata/artwork, and all bundled assets have not been comprehensively audited. | Keep media out of the APK/repository unless licensed; review source and rights terms for every service and asset. |
| High | A complete public privacy policy and exact Android backup/deletion behavior have not been established. | Verify backup rules, provide a real user contact path, and publish accurate privacy disclosures before store submission. |
| High | The private signing key is local to the maintainer; backup/restore has not been verified. | Make and test encrypted off-device backups. Never commit the keystore or signing properties. |
| High | Full physical-device release regression was not performed. | Run the cases in [TEST_PLAN.md](TEST_PLAN.md), including stream, seek, background, notification, Bluetooth, clean-install, and upgrade checks. |
| High | Lint has remaining findings; dependency advisories and transitive license inventory were not scanned. | Review lint output; run vulnerability, SBOM, and license scans against the exact next release build. |

## Checks completed

| Check | Result | Limit |
| --- | --- | --- |
| Unit tests | Pass: 72 tests, no failures/errors recorded | Unit tests do not establish real provider behavior or device playback reliability. |
| Lint | Pass with 49 warnings and 1 hint; no errors | Warnings remain to review. |
| Debug/release build | `assembleDebug`, `assembleRelease`, and `bundleRelease` succeeded | Build success does not establish provider authorization or store readiness. |
| Artifact signatures | APK verified with `apksigner`; AAB verified with `jarsigner` | The signed release was not installed and tested on a clean device. |
| GitHub publication | Release workflow completed successfully for `v1.0.0`; APK and checksum are listed as assets | No Play Store submission or approval. |
| UI smoke check | Debug app installed/launched for visual UI review | No full functional playback, lifecycle, network failure, or accessibility regression. |
| Manifest | Launcher activity is exported; playback service is not; permissions include Internet and media playback foreground service | Static manifest review only; does not replace runtime testing. |
| Network/log review | HTTPS endpoints observed; selected search/error log content was removed | Not a TLS test or exhaustive release-log capture. Some diagnostics still include track IDs, hostnames, and response metadata. |
| Source secret scan | Targeted pattern scan found public InnerTube client identifiers in source/history; no private signing key is in the repository | Not a full Git-history scan or dedicated Gitleaks/TruffleHog audit. Public client identifiers are not proof of authorization. |
| Dependencies/licenses | Direct dependencies are described in `CREDITS.md` and `THIRD_PARTY_NOTICES.md` | No generated complete transitive SBOM/license report or vulnerability scan. |

## Maintainer actions

1. Resolve provider authorization and media rights before further distribution.
2. Protect and back up the signing key; verify future artifacts use the same signer.
3. Verify Android backup behavior and publish a complete privacy policy with a private contact method.
4. Review lint warnings; run dependency vulnerability, SBOM, and license scans.
5. Complete physical-device regression and accessibility checks before another release.
6. Keep current release notes, source tag, checksums, and test evidence together.

See [release process](RELEASE_PROCESS.md), [risk register](RISK_REGISTER.md), [privacy inventory](PRIVACY.md), and [test plan](TEST_PLAN.md) for the detailed follow-up.
