# Architecture

EchoWave is a single-module Android application with explicit domain, data, playback, and UI packages. `AppContainer` is the manual composition root.

## Runtime flow

```text
Compose UI -> ViewModels -> search/ranking use cases -> MusicRepository -> MusicSource
                                                                      -> StreamResolver chain
PlaybackController <-> Media3 session/service <-> ExoPlayer
Room: library tracks and playlists     DataStore: settings and recent searches
```

## User-selected local audio

The Library's **On device** tab imports audio through Android's system document picker. `LocalAudioRepositoryImpl` copies selected files to `getExternalFilesDir(MUSIC)/EchoWave/Downloads` (with an internal app-files fallback) and derives a `Track` from local media metadata. Audio bytes are ordinary files; no audio payload is stored in Room. The folder is app-specific and Android removes it on uninstall; backup/restore follows the app's Android backup configuration. The implementation does not fetch audio from catalog providers.

When the existing `MusicRepository` resolves a `local-audio:` ID, it asks `LocalAudioRepository` for the app-owned file URI and returns a missing-file error if it no longer exists. `PlaybackService`'s `LocalAwareDataSource` sends `file:` and `content:` URIs to Media3's default data source while keeping HTTP(S) playback on the existing bounded, cached stream path. UI still controls playback through `PlaybackController`.

- **UI:** Compose screens live under `ui/`. ViewModels expose screen state and actions. Screens observe playback through `PlaybackController`; they do not access ExoPlayer directly.
- **Domain:** `domain/model`, `domain/repository`, `domain/source`, and `domain/usecase` hold app contracts and playback data types.
- **Data:** `data/local` contains Room and DataStore repositories. `data/remote/innertube` contains the current unofficial provider client. `data/remote/lyrics` contains lyrics clients. Fallback resolvers currently include placeholders and should not be treated as operational services.
- **Playback:** `PlaybackService` owns the Media3 session. `Media3PlaybackController` translates app actions/state; `PlaybackSessionConnector` connects UI lifecycle to the long-lived service session.
- **Composition:** `AppContainer` constructs and binds repositories, the active source, stream resolver chain, settings, and playback controller.
- **Search:** InnerTube parsing produces typed search cards and rows. `SearchResultRanker` applies pure `TextMatch` policy in the domain layer; the selected provider ID is resolved through `MusicRepository` and the playback controller.
- **Recommendations:** `domain/recommendation` defines the candidate pipeline and scoring engine:
  - **Multi-tier caching:** Tier 1 audio stream disk LRU cache (500MB via `MediaCacheManager` wrapping Media3 data sources) and Tier 2 candidate metadata LRU cache (2h TTL via `CandidateCache`).
  - **Candidate sources:** `InnerTubeRadioCandidateSource` requests seed-based candidates from the existing undocumented YouTube Music `/next` integration; `LibraryCandidateSource` can contribute local library tracks.
  - **Feature extraction:** `TrackFeatureExtractor` detects era, Indic/global transliterated language heuristics, and stylistic mood markers (`DANCE_PARTY`, `ROMANTIC_MELODIC`, `ACOUSTIC_CHILL`, etc.).
  - **Taste tracking:** `SessionTasteTracker` maintains an 8-track recency-weighted session profile with rapid-skip fatigue detection; recommendations also receive the local DNA snapshot, favorites, and recent library history.
  - **5-factor scoring model:** Session (30%), DNA (25%), Similarity (20%), History (15%), Discovery (10%) with repetition and fatigue penalties.
  - **Playback orchestration:** Search taps start a one-track seed queue; the controller prefetches and pre-resolves radio candidates while it plays. Explicit playlist/library queues remain sequential. If an in-flight recommendation request finishes after the seed changes, its results are discarded and the new seed is fetched.
  - **Echo This:** The current `PlaybackController` builds the same candidate context and returns ranked results with the scorer's explanation without mutating playback state. The player opens a review screen; each selected result is appended through the controller's existing queue API. Reasons come from detected style/language/era matches, local DNA artist evidence, and favorite status. Missing metadata is stated as a limitation; it does not earn a fresh-match boost. DNA artist/language influence scales with the number of supporting local events, so a single play has low confidence.
  - **Provider boundary:** The radio source is unofficial and not documented as authorized; local scoring is EchoWave code and does not send local listening history/profile to YouTube. See [InnerTube notes](docs/INNER_TUBE_NOTES.md) and [privacy inventory](docs/PRIVACY.md).
- **Listening Summary:** `data/local/ListeningSummaryRepositoryImpl` reads retained Room playback outcomes; `features/listening/ListeningSummaryAggregator` computes calendar-period totals and rankings locally. Raw events remain subject to the existing 12-month retention. Time-of-day breakdown is hidden unless enabled by the user; genres are omitted because current local track records do not contain verified genre metadata.
- **Music DNA display:** Listening Summary observes the aggregate `MusicDnaProfile` and enabled setting from `DnaRepository`. It shows only populated artist/language signals, marks early profiles as still forming, hides day-part information unless the existing opt-in is enabled, and does not invent genre or mood data.
- **Seeking:** Media3 reads through a 500 MiB disk LRU cache above the bounded-range data source. Load control uses a shorter 1.5-second rebuffer threshold after seek/rebuffer; verify actual seek latency on physical devices and varied networks.
- **AI boundary:** `features/ai` is an extension interface; current wiring uses a no-op provider. Network AI must not be called from playback or persistence code.

## Change boundaries

1. Add a catalog source by implementing `domain/source/MusicSource.kt`; bind it through `AppContainer`.
2. Add a stream provider by implementing `domain/source/StreamResolver.kt`; append it to the resolver chain in `AppContainer` only after its terms, reliability, and attribution are reviewed.
3. Preserve a single queue/playback owner. Do not branch playback behavior on provider names in UI code.
4. Keep persistence behind repository classes. Room stores library tracks/playlists; DataStore stores settings and recent searches.
5. Provide loading, empty, and recoverable error states for new user flows.
6. Attribute reused code/assets and update `CREDITS.md` and `THIRD_PARTY_NOTICES.md`.
7. Use `TrackIdentity` for metadata-based duplicates; preserve meaningful song versions while ignoring promotional title labels.

## Priority

- **P0:** search-to-play, stream resolution, Media3 playback, session/notification controls, queue, seek, previous/next, service lifecycle.
- **P1:** favorites, history, playlists, shuffle/repeat, settings, themes.
- **P2:** lyrics, optional integrations, downloads, EQ, Android Auto, casting, additional providers, AI.

Do not trade P0 playback reliability for lower-priority features. The current provider integration has unresolved terms risk; see [InnerTube notes](docs/INNER_TUBE_NOTES.md).
