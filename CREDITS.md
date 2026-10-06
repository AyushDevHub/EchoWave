# Credits and Attributions

EchoWave is distributed under [GNU GPL-3.0](LICENSE). `NOTICE` records the concise project notice; [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) summarizes dependency and inventory status.

## Donor code: Echo-Music

[EchoMusicApp/Echo-Music](https://github.com/EchoMusicApp/Echo-Music) is the primary donor for selected InnerTube stream and PO-token implementation approaches. The donor project is licensed under GPL-3.0. EchoWave includes and adapts the following material:

- `app/src/main/assets/po_token.html` is vendored from Echo-Music. Its attribution header is retained.
- InnerTube client ordering, request identity/header policy, stream resolution, and BotGuard/PO-token handling in `data/remote/innertube/` and `data/remote/potoken/` adapt the donor's implementation approach. EchoWave's Kotlin files are its own implementations unless comments say otherwise.
- Bounded range fetching and request dressing in `data/playback/` follow the donor stream pipeline approach.

Review source-file headers and the GPL-3.0 license when modifying or redistributing these portions.

## Service and API integrations

- The app currently calls unofficial YouTube/YouTube Music InnerTube endpoints. This is not an endorsement or authorization. See [InnerTube notes](docs/INNER_TUBE_NOTES.md).
- Lyrics lookup uses LyricsPlus-compatible services and LRCLIB. [YouLyPlus](https://github.com/ibratabian17/YouLyPlus) and the [LyricsPlus backend](https://github.com/ibratabian17/lyricsplus) are credited as the ecosystem/format reference. EchoWave does not bundle their extension code. Mirrors are independently operated.
- Artwork and music metadata are loaded from remote providers; no remote tracks are included as bundled assets.

## Reference-only projects

- [Spotube](https://github.com/KRTirtho/spotube) — BSD-4-Clause; architecture reference only, no code included.
- [LastWave-native](https://github.com/Clash-Projects/LastWave-native) — GPL-3.0; Kotlin/Compose/Media3 architecture reference only, no code included.
- [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) — GPL-3.0; reference only, not an app dependency and no code included.
- BravePipe — referenced as a possible fallback only; `BravePipeFallbackResolver` is a placeholder and does not contain BravePipe code.
- `innertubex` appears as an inactive version-catalog coordinate. It is not included in the app dependency graph and no source was copied from it.

## Dependencies

Direct runtime, build, and test dependencies are summarized in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Exact versions are in `gradle/libs.versions.toml` and `app/build.gradle.kts`. The list is not a generated transitive SBOM; maintainers should generate a resolved dependency/license inventory before store distribution.

## Trademarks

YouTube, YouTube Music, Google, Spotify, Apple, and other service names are trademarks of their respective owners. EchoWave is an independent project and is not affiliated with or endorsed by these services.
