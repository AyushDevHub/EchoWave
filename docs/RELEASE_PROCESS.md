# Release Process

This document records how the public v1.0.0 APK was assembled and what to do for future releases. GitHub repository releases are public downloads.

## Before the next release

1. Resolve the provider and content-rights items in [RISK_REGISTER.md](RISK_REGISTER.md). A GitHub release does not grant rights or provider authorization.
2. Update `versionCode` and `versionName` in `app/build.gradle.kts`; choose a new release tag, notes file, and APK asset name.
3. Review the open issues and risk register. Update the privacy notice, credits, notices, and changelog for all behavior/dependency/source changes.
4. Run `test`, `lint`, `assembleRelease`, and `bundleRelease`. Review every lint warning and build output.
5. Install the signed release on a clean supported Android device. Run the applicable cases in [TEST_PLAN.md](TEST_PLAN.md), especially playback, seeking, background service, notifications, and upgrade behavior.
6. Verify the APK signature and checksum. Never publish an unsigned artifact or include the signing key in the repository or GitHub Actions logs.
7. Write release notes that state supported Android versions, known limitations, source changes, and provider/rights caveats.

## Signing key

Gradle reads `~/.android/echowave-release.properties` (Windows: `%USERPROFILE%\.android\echowave-release.properties`). The referenced keystore and properties file contain sensitive signing material. Store encrypted backups separately from the development computer and test that a backup can be restored. Do not upload these files as repository artifacts or secrets unless a reviewed CI signing process is introduced.

The v1.0.0 signing key was created locally and is required to sign future updates that install over the existing APK. If that key is lost, a replacement signing identity will not update existing installations; users would have to uninstall and reinstall, losing local app data.

## Current GitHub release workflow

`.github/workflows/publish-apk.yml` runs on the `v1.0.0` tag and creates a GitHub Release with the already-signed APK and its SHA-256 file. It does not build or sign the APK in GitHub Actions. The binary is committed under `release-assets/` so the workflow can publish the exact verified artifact without exposing the private key.

For a future version, update the trigger tag, APK asset, checksum, title, and release-notes path in the workflow before pushing the new tag. Confirm the workflow succeeds and the release page lists both assets. Verify the public download and checksum after publication.

## After publication

- Confirm the GitHub Actions release job succeeded and the asset downloads correctly.
- Keep the tag, release notes, checksum, and source commit associated with the published APK.
- Watch repository notifications, issue reports, and maintainer email for GitHub or provider notices. Keep a current contact email on the GitHub account.
- Triage bug reports using [SUPPORT.md](../SUPPORT.md); never ask users to post tokens or signed URLs.
- If a credible policy or rights complaint arrives, preserve it, review the affected code/assets, and respond through the platform's stated process. Do not evade access blocks by changing accounts, keys, or request identity.
- Fix urgent security or privacy issues and publish a new signed release only after the relevant checks pass.
