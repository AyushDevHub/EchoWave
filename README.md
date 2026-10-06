# EchoWave

**A clean, open source Android music player built with Kotlin, Jetpack Compose, and Media3.**

EchoWave brings search, queue-based playback, and background listening together in a small Android app. It resolves streams from compatible providers and plays them through Android's media session, including notification and lock-screen controls.

> GPL-3.0 licensed. See [LICENSE](LICENSE) and [CREDITS.md](CREDITS.md).

## What it does

| Area | Included |
| --- | --- |
| Search | Search tracks and browse results |
| Playback | Queue, seek, previous and next, shuffle, repeat, and automatic advance |
| Background audio | Media session, system notification, and lock-screen controls |
| Stream handling | Resolver chain, client-specific request headers, and bounded range fetching |
| Library | Local persistence for saved tracks and listening history |

Provider behavior can change independently of the app. Resolver fallbacks are currently scaffolding and may not return a stream.

## Build

Requirements: Android Studio with the project-compatible Android SDK and JDK.

```sh
./gradlew assembleDebug
./gradlew test
```

Install a debug build on a connected device with:

```sh
./gradlew installDebug
```

## Project structure

```text
domain/       playback models, repository contracts, and source interfaces
data/         local storage, provider clients, stream resolution, and playback data sources
playback/     Media3 service, session connection, and queue command routing
features/     user-facing features, including the isolated AI feature boundary
ui/           Compose screens and reusable components
```

The UI observes `PlaybackController.state`; it does not access ExoPlayer directly. Music sources implement `MusicSource`, stream providers implement `StreamResolver`, and `AppContainer` binds them. See [ARCHITECTURE.md](ARCHITECTURE.md) for more detail.

## Attribution

EchoWave includes and adapts work from other projects. The [credits and license notes](CREDITS.md) identify the exact vendored file, ports, dependency status, and reference-only material. In brief:

- `po_token.html` is vendored from **Echo-Music** under GPL-3.0, with its attribution retained.
- Stream-client values and parts of the InnerTube and BotGuard stream pipeline are adapted from **Echo-Music**; relevant Kotlin files identify the port in their headers.
- **NewPipe Extractor** and **BravePipe** are currently names for unimplemented fallback stubs; their code is not included.
- **Spotube** and **LastWave-native** informed architecture choices only; no code from those projects is included.
- The `innertubex` artifact is not an active dependency.

## Legal

EchoWave is an independent project and is not affiliated with Google, YouTube, YouTube Music, Spotify, Apple, or other music services. Service names and trademarks belong to their respective owners. EchoWave does not host or distribute music; users are responsible for complying with applicable laws and service terms. The software is provided under GPL-3.0, without warranty. See [LICENSE](LICENSE), [NOTICE](NOTICE), and [CREDITS.md](CREDITS.md).
