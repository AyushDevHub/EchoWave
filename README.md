# EchoWave

EchoWave is an open source Android music player built with Kotlin, Jetpack Compose, and Media3. It combines search, local playlists and favorites, listening history, lyrics, and background playback controls.

The interface includes multiple color themes, personalized discovery, recent searches, animated word-synced lyrics, and a compact player. Preferences and library data are stored locally.

> GPL-3.0 licensed. See [LICENSE](LICENSE), [NOTICE](NOTICE), and [CREDITS.md](CREDITS.md).

## Important source and distribution notice

Current discovery and playback use unofficial YouTube/YouTube Music InnerTube endpoints. This is not a supported public music API. YouTube's [Developer Policies](https://developers.google.com/youtube/terms/developer-policies) prohibit undocumented API use without express permission and prohibit background audio playback for API Clients. EchoWave's current background player and InnerTube integration therefore have unresolved provider-policy concerns. Review [docs/INNER_TUBE_NOTES.md](docs/INNER_TUBE_NOTES.md) and [docs/SECURITY_AUDIT.md](docs/SECURITY_AUDIT.md) before distributing or using provider-backed features. This repository does not claim that the integration is authorized.

Provider availability, terms, and content rights are separate from EchoWave. The app does not host music. Users must ensure their use complies with applicable terms and law.

## Features

| Area | Included |
| --- | --- |
| Search and discovery | Search, browse collections, moods and genres, and recent searches |
| Playback | Queue, seek, previous and next, shuffle, repeat, and automatic advance |
| Lyrics | Lyrics lookup and word-timed lyric presentation when timestamps are available |
| Library | Local favorites, playlists, and listening history |
| Personalization | Optional name greeting, taste preferences, and selectable themes |
| System controls | Media session, notification, and lock-screen playback controls |

Music source and fallback behavior can change independently of EchoWave; some fallback resolvers are currently scaffolding.

## Build

Requirements: Android Studio with the project-compatible Android SDK and JDK.

```sh
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

Install a debug build on a connected device:

```sh
./gradlew installDebug
```

## Build a locally signed release APK

Release signing keys must stay private and must be backed up securely. Gradle reads an optional properties file at `~/.android/echowave-release.properties` (Windows: `%USERPROFILE%\.android\echowave-release.properties`). Example:

```properties
storeFile=/absolute/path/to/echowave-release.jks
storePassword=YOUR_PRIVATE_STORE_PASSWORD
keyAlias=echowave
keyPassword=YOUR_PRIVATE_KEY_PASSWORD
```

Keep both the keystore and this properties file out of Git. Losing the keystore prevents signing future updates with the same identity. With this file present, run:

```sh
./gradlew assembleRelease bundleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`; the Play bundle is at `app/build/outputs/bundle/release/app-release.aab`. Without the private signing file Gradle produces unsigned release artifacts that are not ready for distribution. A signed build is not, by itself, a provider, privacy, store, or legal approval; see [the release audit](docs/SECURITY_AUDIT.md).

## Project structure

```text
domain/       playback models, repository contracts, and source interfaces
data/         local storage, provider clients, stream resolution, playback data sources
playback/     Media3 service, session connection, and queue command routing
features/     user-facing features, including the isolated AI feature boundary
ui/           Compose screens and reusable components
```

The UI observes `PlaybackController.state`; it does not access ExoPlayer directly. Music sources implement `MusicSource`, stream providers implement `StreamResolver`, and `AppContainer` binds them. See [ARCHITECTURE.md](ARCHITECTURE.md).

## Attribution and licenses

See [CREDITS.md](CREDITS.md) for donor code, reference-only projects, and direct dependencies. [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) records the current inventory limits. GPL-3.0 source license: [LICENSE](LICENSE).

## Project status

The repo includes [setup notes](docs/SETUP.md), [test plan](docs/TEST_PLAN.md), [privacy implementation notes](docs/PRIVACY.md), and [security/release audit](docs/SECURITY_AUDIT.md). These are engineering records, not a published privacy policy or a complete security, legal, or release certification.
