# Architecture

EchoWave is a single-module Android application with explicit domain, data, playback, and UI packages. `AppContainer` is the manual composition root.

## Runtime flow

```text
Compose UI -> ViewModels -> use cases / repositories -> MusicSource
                                                   -> StreamResolver chain
PlaybackController <-> Media3 session/service <-> ExoPlayer
Room: library tracks and playlists     DataStore: settings and recent searches
```

- **UI:** Compose screens live under `ui/`. ViewModels expose screen state and actions. Screens observe playback through `PlaybackController`; they do not access ExoPlayer directly.
- **Domain:** `domain/model`, `domain/repository`, `domain/source`, and `domain/usecase` hold app contracts and playback data types.
- **Data:** `data/local` contains Room and DataStore repositories. `data/remote/innertube` contains the current unofficial provider client. `data/remote/lyrics` contains lyrics clients. Fallback resolvers currently include placeholders and should not be treated as operational services.
- **Playback:** `PlaybackService` owns the Media3 session. `Media3PlaybackController` translates app actions/state; `PlaybackSessionConnector` connects UI lifecycle to the long-lived service session.
- **Composition:** `AppContainer` constructs and binds repositories, the active source, stream resolver chain, settings, and playback controller.
- **AI boundary:** `features/ai` is an extension interface; current wiring uses a no-op provider. Network AI must not be called from playback or persistence code.

## Change boundaries

1. Add a catalog source by implementing `domain/source/MusicSource.kt`; bind it through `AppContainer`.
2. Add a stream provider by implementing `domain/source/StreamResolver.kt`; append it to the resolver chain in `AppContainer` only after its terms, reliability, and attribution are reviewed.
3. Preserve a single queue/playback owner. Do not branch playback behavior on provider names in UI code.
4. Keep persistence behind repository classes. Room stores library tracks/playlists; DataStore stores settings and recent searches.
5. Provide loading, empty, and recoverable error states for new user flows.
6. Attribute reused code/assets and update `CREDITS.md` and `THIRD_PARTY_NOTICES.md`.

## Priority

- **P0:** search-to-play, stream resolution, Media3 playback, session/notification controls, queue, seek, previous/next, service lifecycle.
- **P1:** favorites, history, playlists, shuffle/repeat, settings, themes.
- **P2:** lyrics, optional integrations, downloads, EQ, Android Auto, casting, additional providers, AI.

Do not trade P0 playback reliability for lower-priority features. The current provider integration has unresolved terms risk; see [InnerTube notes](docs/INNER_TUBE_NOTES.md).
