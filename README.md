# EchoWave

[![Android CI](https://github.com/AyushDevHub/EchoWave/actions/workflows/android.yml/badge.svg)](https://github.com/AyushDevHub/EchoWave/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/github/v/release/AyushDevHub/EchoWave)](https://github.com/AyushDevHub/EchoWave/releases/latest)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

EchoWave is an Android music player built with Kotlin, Jetpack Compose, and AndroidX Media3. It includes search and discovery, local playlists and favorites, listening history, lyrics, background playback controls, personal themes, and a compact player.

## Download

The current public APK is on the [GitHub Releases page](https://github.com/AyushDevHub/EchoWave/releases/latest). Release notes include installation information and a SHA-256 checksum. EchoWave requires Android 8.0 (API 26) or newer.

## Important provider and distribution notice

EchoWave currently uses unofficial YouTube/YouTube Music InnerTube endpoints for search, discovery, metadata, and stream resolution. These are undocumented interfaces and may stop working without notice. EchoWave is not an official YouTube client and is not affiliated with or endorsed by Google or YouTube.

YouTube's [Terms of Service](https://www.youtube.com/t/terms) restrict automated access without prior written permission or another applicable basis. Its [API Services Developer Policies](https://developers.google.com/youtube/terms/developer-policies) prohibit undocumented API use without express permission and prohibit background playback in API Clients. How those terms apply to this particular integration is a legal question; EchoWave has no documented authorization. A disclaimer, open-source license, or another app using similar methods does not grant permission. See [provider integration notes](docs/INNER_TUBE_NOTES.md) and the [project risk register](docs/RISK_REGISTER.md).

The app does not host music. Users are responsible for their own use of the app and third-party services. Track artwork, metadata, lyrics, and streams are provided by independent services and rights holders.

## Features

| Area | Features |
| --- | --- |
| Discovery | Search, browse collections, moods and genres, recent searches, taste-led suggestions |
| Playback | Queue, seek, previous/next, shuffle, repeat, and automatic advance |
| Lyrics | Lyrics lookup and animated word-timed display when timing data is available |
| Library | Local favorites, playlists, and listening history |
| Personalization | Optional name greeting and selectable color themes |
| Android integration | Media session, notification, lock-screen, and background playback controls |

Provider behavior can change independently of EchoWave. Some fallback resolvers are placeholders and should not be presented as working providers.

## Build from source

Requirements: Android Studio, Android SDK Platform 37, and JDK 17. Open the project in Android Studio or use the Gradle wrapper from the repository root.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lint
```

Install a debug build on a connected device:

```powershell
.\gradlew.bat installDebug
```

See [developer setup](docs/SETUP.md) for details.

## Release signing

Release signing keys are private and must never be committed. For a local release build, Gradle reads `~/.android/echowave-release.properties` (Windows: `%USERPROFILE%\.android\echowave-release.properties`). Keep the properties file and referenced keystore private, and maintain a secure backup. Losing the key prevents signing future updates with the same identity.

```powershell
.\gradlew.bat assembleRelease bundleRelease
```

The [release process](docs/RELEASE_PROCESS.md) describes versioning, verification, and publishing. The public v1.0.0 APK is a signed release artifact; it has not been fully regression-tested on a clean physical device. A successful build does not establish provider authorization, privacy compliance, or store approval.

## Project structure

```text
app/src/main/java/com/howdy/echowave/
  domain/       models, repository contracts, sources, and use cases
  data/         local persistence, provider clients, and stream resolution
  playback/     Media3 service, session, controller, and queue routing
  features/     optional feature boundaries, including AI extension interface
  ui/           Compose screens, navigation, and theme
```

The UI observes `PlaybackController` state and does not access ExoPlayer directly. `AppContainer` wires repositories, music sources, resolvers, and playback.

## Project documentation

- [Documentation index](docs/INDEX.md)
- [Architecture](ARCHITECTURE.md)
- [Release process and post-release checklist](docs/RELEASE_PROCESS.md)
- [Risk register](docs/RISK_REGISTER.md)
- [Privacy and data inventory](docs/PRIVACY.md)
- [Security policy](SECURITY.md)
- [Security and release review](docs/SECURITY_AUDIT.md)
- [Provider integration notes](docs/INNER_TUBE_NOTES.md)
- [Credits and dependency notices](CREDITS.md), [third-party notices](THIRD_PARTY_NOTICES.md)
- [Changelog](docs/CHANGELOG.md)

## Project policies

Please read the [Code of Conduct](CODE_OF_CONDUCT.md), [contributor guide](CONTRIBUTING.md), [support guide](SUPPORT.md), and issue/PR templates before interacting with the repository. These files describe project expectations; they do not constitute provider authorization or alter third-party service terms.

## License

EchoWave is licensed under the [GNU General Public License v3.0](LICENSE). Donor code, dependency, and asset notes are in [CREDITS.md](CREDITS.md) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The app is provided without warranty; see the license for details.
