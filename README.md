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
| Onboarding | First-run setup for listener name, genre picks, and artist preferences |
| Discovery | Search with typed filters, album detail track lists, browse collections, moods and genres, recent searches, and seed-based radio autoplay |
| Music DNA | On-device listening taste profile, day-part awareness, and multi-factor radio recommendations based on the current seed, recent listening, favorites, and library history |
| Playback | Queue, seek, previous/next, shuffle, repeat, and automatic advance |
| Lyrics | Lyrics lookup and animated word-timed display when timing data is available |
| Library | Local favorites, playlists, and listening history |
| Personalization | Selectable color themes (12 presets), custom typography (Outfit / Plus Jakarta Sans), player styles (Classic, Glow, Contrast), and optional personalized greeting |
| Android integration | Media session, notification, lock-screen, and background playback controls |

Provider behavior can change independently of EchoWave. Some fallback resolvers are placeholders and should not be presented as working providers.

## Credits and provider status

EchoWave adapts selected InnerTube stream and PO-token approaches from [Echo-Music](https://github.com/EchoMusicApp/Echo-Music); the vendored asset and adapted areas are listed in [CREDITS.md](CREDITS.md). The project also credits its text-matching code port and other reference-only projects there. EchoWave's recommendation ranking and on-device listening profile are project code; local listening events and the profile are not sent with radio requests. See [third-party notices](THIRD_PARTY_NOTICES.md) for dependencies and [privacy notes](docs/PRIVACY.md) for data handling.

The current YouTube/YouTube Music integration uses undocumented endpoints and has no documented authorization. Three MP3 files are also included in the Android resources; their origin and distribution rights are unknown. The next APK release remains blocked pending the provider and content-rights review described in the [risk register](docs/RISK_REGISTER.md).

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

The [release process](docs/RELEASE_PROCESS.md) describes versioning, verification, and publishing. The public v1.0.0 APK is the latest published artifact. A v1.0.1 candidate has been tested on a Moto G96 5G for lyrics synchronization and track changes; full device regression is incomplete, and the release checklist's provider authorization and content-rights prerequisite remains unresolved. Build success does not establish provider authorization, privacy compliance, or store approval.

## Project structure

```text
app/src/main/java/com/howdy/echowave/
  core/         script-agnostic text matching and network error handling
  domain/       models, repository contracts, sources, recommendations, and use cases
  data/         local persistence, provider clients, and stream resolution
  playback/     Media3 service, session, controller, and queue routing
  features/     feature boundaries, including Music DNA and AI extension interface
  ui/           Compose screens, navigation, and theme
```

The UI observes `PlaybackController` state and does not access ExoPlayer directly. `AppContainer` wires repositories, music sources, resolvers, and playback.

## Project documentation

- [Documentation index](docs/INDEX.md)
- [Architecture](ARCHITECTURE.md)
- [Developer setup](docs/SETUP.md)
- [Test plan](docs/TEST_PLAN.md)
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
