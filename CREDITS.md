# Credits and Attributions

EchoWave is distributed under [GNU GPL-3.0](LICENSE). `NOTICE` records the concise project notice; [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) summarizes dependency and inventory status.

## Donor code: Echo-Music

[EchoMusicApp/Echo-Music](https://github.com/EchoMusicApp/Echo-Music) is the primary donor for selected InnerTube stream and PO-token implementation approaches. The donor project is licensed under GPL-3.0. EchoWave includes and adapts the following material:

- `app/src/main/assets/po_token.html` is vendored from Echo-Music. Its attribution header is retained.
- InnerTube client ordering, request identity/header policy, stream resolution, and BotGuard/PO-token handling in `data/remote/innertube/` and `data/remote/potoken/` adapt the donor's implementation approach. EchoWave's Kotlin files are its own implementations unless comments say otherwise.
- Search filter params (`domain/model/SearchFilter.kt`), the card-shelf top-result and shelf-grouped summary approach (`parseCardShelf`, `SearchSummary` handling in `InnerTubeMappers.kt`) adapt Echo-Music `YouTube.SearchFilter` and `SearchSummaryPage`. Param strings are used verbatim.
- Bounded range fetching and request dressing in `data/playback/` follow the donor stream pipeline approach.

Review source-file headers and the GPL-3.0 license when modifying or redistributing these portions.

## Service and API integrations

- The app currently calls unofficial YouTube/YouTube Music InnerTube endpoints. This is not an endorsement or authorization. See [InnerTube notes](docs/INNER_TUBE_NOTES.md).
- `InnerTubeRadioCandidateSource` consumes candidate metadata from the existing undocumented YouTube Music `/next` flow. Candidate filtering, EchoWave's local feature extraction/scoring, queue behavior, and on-device DNA aggregation are EchoWave implementations; no YouTube recommendation-model code or weights are included. The provider integration is not documented as authorized.
- Lyrics lookup uses LyricsPlus-compatible services and LRCLIB. [YouLyPlus](https://github.com/ibratabian17/YouLyPlus) and the [LyricsPlus backend](https://github.com/ibratabian17/lyricsplus) are credited as the ecosystem/format reference. EchoWave does not bundle their extension code. Mirrors are independently operated.
- A compact update chip reads public release metadata from GitHub's REST API. No third-party updater library or APK installer code is used; users open the release page and install the APK themselves.
- Artwork and music metadata are loaded from remote providers; no remote tracks are included as bundled assets.

## Reference-only projects

- [Spotube](https://github.com/KRTirtho/spotube) — BSD-4-Clause; architecture reference only, no code included.
- [LastWave-native](https://github.com/Clash-Projects/LastWave-native) — GPL-3.0; Kotlin/Compose/Media3 architecture reference, plus ported text-matching: `core/common/TextMatch.kt` ports their `data/music/TextMatch.kt` scoring shape (Unicode normalization, noise-word removal, weighted similarity) with an adapted candidate signature.
- [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) — GPL-3.0; reference only, not an app dependency and no code included.
- BravePipe — referenced as a possible fallback only; `BravePipeFallbackResolver` is a placeholder and does not contain BravePipe code.
- `innertubex` appears as an inactive version-catalog coordinate. It is not included in the app dependency graph and no source was copied from it.

## Dependencies

Direct runtime, build, and test dependencies are summarized in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Exact versions are in `gradle/libs.versions.toml` and `app/build.gradle.kts`. The list is not a generated transitive SBOM; maintainers should generate a resolved dependency/license inventory before store distribution.

## Fonts and trademarks

- Outfit and Plus Jakarta Sans are bundled in `app/src/main/res/font/` under the SIL Open Font License; see `THIRD_PARTY_NOTICES.md`.
- Google Sans is proprietary and is not bundled; the "Google Sans (system)" option uses the platform sans as a stand-in.
- Apple Music / Echo-Music are interaction inspirations only; all player and lyric UI here is original code, no copied assets.
- Spotify, Apple Music, YouTube Music, LastWave, and Echo Music were discussed as behavioral references for music discovery; EchoWave does not include their recommendation code, models, catalog, or assets.

## Removed audio resources

`app/src/main/res/raw/idiots.mp3`, `desi_girl.mp3`, and `chamak_challo.mp3` were unused and have been removed from the current source tree. Their origin, copyright owner, and license/permission were not recorded. Earlier Git revisions still contain the files; this deletion does not remove them from repository history. The current APK candidate must be rebuilt and checked to confirm it excludes them.

## Trademarks

YouTube, YouTube Music, Google, Spotify, Apple, and other service names are trademarks of their respective owners. EchoWave is an independent project and is not affiliated with or endorsed by these services.
