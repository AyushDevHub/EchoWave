# EchoWave Architecture

## Layers

```text
ui/ -> ViewModel -> usecase -> repository -> source
playback/ -> PlaybackService (MediaSessionService) + Media3PlaybackController
data/remote/innertube|fallback|fake, data/local (Room + DataStore)
features/ai/ — extension only, depends on domain, never depended upon
```

## Expansion rules (AI / UI / extras must not break core)

1. Add providers by implementing `MusicSource` or `StreamResolver`, register in
   `AppContainer.kt`. Never branch playback on provider names.
2. Add AI by implementing `features/ai/RecommendationProvider`. Never call AI
   from `playback/` or `data/remote/`.
3. Add UI by adding routes in `ui/navigation/Routes.kt` + screens under `ui/`.
   Screens read `PlaybackController.state`, never ExoPlayer.
4. Stream chain is `ChainedStreamResolver(primary, newpipe, bravepipe)`.
   Reorder in AppContainer, not in UI.
5. Persistence: Room `TrackEntity` for favorites/history; DataStore prefs for
   settings. No new store without a repository interface first.

## P0 / P1 / P2

P0: search, metadata, resolve, ExoPlayer, MediaSession, background,
notification, lock, mini-player, Now Playing, queue, seek, prev/next.
P1: favorites, history, shuffle, repeat, themes, basic playlists, settings.
P2: lyrics, downloads, EQ, Auto, cast, multi-provider, AI.
