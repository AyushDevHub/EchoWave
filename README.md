# EchoWave

Clean, open-source Android music player. Kotlin + Compose + Material 3 + Media3.

MVP: search music from a YouTube-Music-compatible source, resolve playable
streams, play reliably in background with notification / lock-screen, modern
mini-player + Now Playing + queue.

EchoWave is GPL-3.0. See `LICENSE` and `CREDITS.md`.
APK distribution via GitHub Releases (no Play Store).

## MVP flow

Open -> Home -> Search -> results -> tap -> resolve stream -> ExoPlayer ->
mini-player -> Now Playing -> background -> notification/lock.

## Build

```sh
./gradlew assembleDebug
```

## Architecture

See `ARCHITECTURE.md`. Rule:

```text
SEARCH/METADATA != STREAM RESOLUTION != PLAYBACK != UI
```

UI observes `PlaybackController.state`. Never touches ExoPlayer.
Sources live behind `MusicSource` + `StreamResolver` (chained primary + fallbacks).
AI lives in `features/ai/` and depends on core, never the reverse.
