# Release Process

This document records how EchoWave APK releases are assembled. GitHub repository releases are public downloads. Version 1.0.1 was published on 2026-10-10 while the provider authorization and content-rights questions were unresolved. This was a maintainer decision and does not establish permission, rights, or provider approval. Those critical risks remain open for future releases.

## Before the next release

1. Resolve the provider and content-rights items in [RISK_REGISTER.md](RISK_REGISTER.md). YouTube's [published API Services Developer Policies](https://developers.google.com/youtube/terms/developer-policies) say undocumented APIs must not be used without express permission. Three unused MP3 resources with undocumented provenance were removed from the current source tree; earlier Git revisions retain them. Review whether the historical files may remain public. The register records undocumented YouTube/YouTube Music access and uncleared rights as critical risks; obtain written permission or replace the integration with permitted sources before the next distribution. A GitHub release does not grant rights or provider authorization. These risks remain unresolved after v1.0.1.
2. Update `versionCode` and `versionName` in `app/build.gradle.kts`; choose a new release tag, notes file, and APK asset name.
3. Review the open issues and risk register. Update the privacy notice, credits, notices, and changelog for all behavior/dependency/source changes.
4. Run `test`, `lint`, `assembleRelease`, and `bundleRelease`. Review every lint warning and build output.
5. Install the signed release on a clean supported Android device. Run the applicable cases in [TEST_PLAN.md](TEST_PLAN.md), especially playback, seeking, background service, notifications, and upgrade behavior.
6. Verify the APK signature and checksum. Never publish an unsigned artifact or include the signing key in the repository or GitHub Actions logs.
7. Write release notes that state supported Android versions, known limitations, source changes, and provider/rights caveats.

## v1.0.1 release status (2026-10-10)

- Version set to 1.0.1 / versionCode 2 so an eventual signed update can upgrade v1.0.0.
- The Home screen now checks GitHub's latest release metadata and displays a compact update chip when a newer APK is available. The first release containing this checker still needs to be announced manually; future availability is surfaced while the app is open.
- Lyrics track-change and playback checks were run on the Moto G96 5G using a debug build; see [TEST_PLAN.md](TEST_PLAN.md). This is not a full release-device regression.
- The earlier debug APK packaged `idiots.mp3`, `desi_girl.mp3`, and `chamak_challo.mp3`; the files were unused and have now been removed from the source tree. Rebuild and inspect the candidate APK before any distribution. Their origin and distribution rights remain unknown, and earlier Git revisions retain them.
- A signed 1.0.1 APK was built locally on 2026-10-10. Its signature verifies and matches the v1.0.0 certificate; the APK archive contains none of the three removed MP3 files. The debug build received a limited device smoke check; full signed-release device regression was not completed. Current unit/lint/build evidence is recorded in [TEST_PLAN.md](TEST_PLAN.md).
- Version 1.0.1 was published despite the unresolved provider authorization and content-rights review. The release notes disclose this status. The next release should not proceed until the critical risks are resolved.

## Signing key

Gradle reads `~/.android/echowave-release.properties` (Windows: `%USERPROFILE%\.android\echowave-release.properties`). The referenced keystore and properties file contain sensitive signing material. Store encrypted backups separately from the development computer and test that a backup can be restored. Do not upload these files as repository artifacts or secrets unless a reviewed CI signing process is introduced.

The v1.0.0 signing key was created locally and is required to sign future updates that install over the existing APK. If that key is lost, a replacement signing identity will not update existing installations; users would have to uninstall and reinstall, losing local app data.

## Current GitHub release workflow

`.github/workflows/publish-apk.yml` runs on push to any `v*` tag and creates a GitHub Release with the matching version's signed APK (`release-assets/EchoWave-<version>.apk`), checksum file, and release notes (`docs/releases/<version>.md`). It does not build or sign the APK in GitHub Actions. The signed binary is placed under `release-assets/` so the workflow can publish the verified artifact without exposing the private key.

For a future version, place the signed `EchoWave-<version>.apk` and its checksum into `release-assets/`, add `docs/releases/<version>.md`, and push the new `v<version>` tag. Confirm the workflow succeeds and the release page lists the assets. Verify the public download and checksum after publication.

## After publication

- Confirm the GitHub Actions release job succeeded and the asset downloads correctly.
- Keep the tag, release notes, checksum, and source commit associated with the published APK.
- Watch repository notifications, issue reports, and maintainer email for GitHub or provider notices. Keep a current contact email on the GitHub account.
- Triage bug reports using [SUPPORT.md](../SUPPORT.md); never ask users to post tokens or signed URLs.
- If a credible policy or rights complaint arrives, preserve it, review the affected code/assets, and respond through the platform's stated process. Do not evade access blocks by changing accounts, keys, or request identity.
- Fix urgent security or privacy issues and publish a new signed release only after the relevant checks pass.
