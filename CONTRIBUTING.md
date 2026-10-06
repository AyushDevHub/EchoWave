# Contributing to EchoWave

Thanks for considering a change. This guide keeps reports and patches focused on the current app and its playback reliability.

## Before you open an issue or pull request

- Search existing issues and pull requests for duplicates.
- Read the [Code of Conduct](CODE_OF_CONDUCT.md), [architecture](ARCHITECTURE.md), [setup guide](docs/SETUP.md), and [provider notes](docs/INNER_TUBE_NOTES.md).
- Do not include secrets, account data, signed stream URLs, copyrighted media, or unredacted logs.
- For a security concern, use the private process in [SECURITY.md](SECURITY.md), not a public issue.

## Issues

Use the GitHub issue forms for bug reports and feature requests. Include enough context to reproduce a bug: app version, Android version/device, expected behavior, actual behavior, and concise steps. Confirm that logs and screenshots do not expose private information.

## Pull requests

Keep each pull request focused and explain the user-visible effect and implementation approach. Link related issues when applicable. Update documentation and attribution when behavior, provider use, dependencies, or donor code changes.

Before submitting, run the checks relevant to the change:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

For playback, source, resolver, and persistence changes, include manual/device checks in the PR description. Do not claim a playback scenario passed unless it was actually run. See the [PR template](.github/PULL_REQUEST_TEMPLATE.md) and [test plan](docs/TEST_PLAN.md).

## Project constraints

- Playback reliability takes priority over new features.
- UI reads playback state from `PlaybackController`; UI code must not access ExoPlayer directly.
- Implement new music sources in `domain/source/MusicSource.kt`, new stream providers in `domain/source/StreamResolver.kt`, and bind them in `AppContainer.kt`.
- Keep AI calls within `features/ai/`; do not call AI from playback or data layers.
- Provide loading, empty, and error states for user-facing flows.
- Preserve existing licenses and update `CREDITS.md` and `THIRD_PARTY_NOTICES.md` when reusing or adding third-party material.
- Do not add undocumented provider access or claim a provider has authorized EchoWave without written evidence.

## Review and merging

Maintainers review changes for correctness, scope, tests, accessibility, privacy, and license/attribution impact. A submitted pull request is not guaranteed to be merged. Please keep discussion respectful and follow the [Code of Conduct](CODE_OF_CONDUCT.md).
