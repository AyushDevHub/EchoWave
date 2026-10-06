# EchoWave

Clean, open-source Android music player. Kotlin + Jetpack Compose + Material 3 + Media3.

MVP: search music from a YouTube-Music-compatible source, resolve playable
streams, play reliably in background with notification / lock-screen, modern
mini-player + Now Playing + queue.

EchoWave is GPL-3.0 — see `LICENSE`, `CREDITS.md`, and License below.
APK distribution via GitHub Releases (no Play Store).

## MVP flow

Open -> Home -> Search -> results -> tap -> resolve stream -> ExoPlayer ->
mini-player -> Now Playing -> background -> notification/lock.

## Status

P0 playback verified on-device: search, resolve (measured client catalog +
pre-flight validation), ranged dressed fetch, queue, seek, prev/next,
skip-on-failure advance, auto-advance, background/notification/lock-screen.

## Build

```sh
./gradlew assembleDebug
./gradlew test
```

Install with `./gradlew installDebug`.

## Architecture

See `ARCHITECTURE.md`. The rule:

```text
SEARCH/METADATA != STREAM RESOLUTION != PLAYBACK != UI
```

UI observes `PlaybackController.state` and never touches ExoPlayer. Sources
live behind `MusicSource` + `StreamResolver` (chained primary + fallbacks).
AI lives in `features/ai/` and depends on core, never the reverse.

## Credits

EchoWave stands on open-source work. Full details in `CREDITS.md`.

- **EchoMusicApp/Echo-Music** (GPL-3.0) — primary donor. Stream pipeline
  thinking, InnerTube client catalog values, WebView BotGuard PO-token flow
  (`po_token.html` vendored verbatim + attribution header), cipher/n-transform
  policy, `PlayerClient` fetch dressing, bounded-range fetching, visitorData
  plumbing. Thank you.
- **MetrolistGroup/InnerTubeX** (upstream license applies, GPL-3.0 family) —
  extractor/cipher client catalog reference (`innertubex-android` coordinate
  kept for the stream-pipeline step).
- **TeamNewPipe NewPipeExtractor + PipePipe extractor** (GPL-3.0) — extractor
  lineage the donors (and our fallback concepts) build on.
- **team-spotube/spotube** (BSD-4-Clause) — plugin-style source-abstraction
  concept only; no code reused.
- **Clash-Projects/LastWave-native** (GPL-3.0) — Kotlin + Compose + Media3 +
  Clean Architecture reference for the app skeleton.
- **BravePipe** — credited by Echo-Music; **not used anywhere in EchoWave**.

If we missed an attribution, open an issue and it will be fixed promptly.

## Legal disclaimer & terms of use

1. **Free, open-source, non-commercial.** Educational / personal-use project.
   No ads, no premium features, no subscriptions, no sale.
2. **No hosting.** EchoWave hosts, uploads, and stores no audio, video, or
   copyrighted material. All content stays on its providers' servers and
   belongs to its owners. The app only streams publicly accessible links the
   user requests, much like a specialized browser.
3. **No affiliation.** Not affiliated with or endorsed by Google, YouTube,
   YouTube Music, Spotify, Apple, Last.fm, or any music service. All
   trademarks belong to their respective owners.
4. **Support creators.** If you listen regularly, a YouTube Premium / YouTube
   Music subscription is the way to support artists. This project exists to
   learn modern Android development, not to harm creator revenue.
5. **Your responsibility.** You are responsible for complying with your local
   laws and the Terms of Service of services you access. Software is provided
   "AS IS", without warranty of any kind.
6. **Copyright contact.** We host no media files, so there is nothing to take
   down on that front. For concerns about the code itself, open a GitHub issue
   and it will be addressed.

## License

GPL-3.0 — see `LICENSE`. GitHub APK releases ship alongside the full source,
as the license requires. Donor files keep their headers and terms; see
`CREDITS.md` and `NOTICE`.
