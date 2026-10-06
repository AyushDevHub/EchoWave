# Credits and third-party notices

EchoWave is distributed under the GNU General Public License, version 3. See [LICENSE](LICENSE). This file records code included in EchoWave, code adapted from other projects, dependencies, and reference-only work.

## Included or adapted code

### Echo-Music

[EchoMusicApp/Echo-Music](https://github.com/EchoMusicApp/Echo-Music) is the primary code donor. It is licensed under GPL-3.0. The following material is included or adapted in EchoWave:

- `app/src/main/assets/po_token.html` is vendored verbatim. Its attribution header is retained.
- InnerTube stream resolution policy and client ordering are adapted in `data/remote/innertube/`.
- Client identity and stream-fetch header policy are adapted in `data/remote/innertube/PlayerClient.kt`.
- BotGuard and PO-token handling policy is adapted in `data/remote/potoken/`; `JsCodec` and the WebView provider are EchoWave Kotlin implementations of that policy.
- Signature-cipher and `n`-parameter handling follows the donor's documented resolver approach; implementation comments identify the relevant donor.
- Bounded range fetching and media request dressing in `data/playback/` follow the donor's stream pipeline approach.

EchoWave's own Kotlin files are not verbatim copies unless stated above. Files that identify a donor in their source comments should be read with this attribution and the GPL-3.0 terms.

## Reference-only projects

- [Spotube](https://github.com/KRTirtho/spotube) — BSD-4-Clause. The plugin-style source abstraction was considered as an architectural reference. No Spotube code is included.
- [LastWave-native](https://github.com/Clash-Projects/LastWave-native) — GPL-3.0. Kotlin, Compose, Media3, and layered architecture were considered as references. No LastWave-native code is included.
- [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) — GPL-3.0. Its extractor ecosystem is acknowledged as background for a possible fallback; no NewPipe code or dependency is currently included.
- BravePipe — a possible fallback concept mentioned in donor research. EchoWave's `BravePipeFallbackResolver` is only an unimplemented stub; no BravePipe code is included.
- [YouLyPlus](https://github.com/ibratabian17/YouLyPlus) and its [LyricsPlus backend](https://github.com/ibratabian17/lyricsplus) are credited as the source ecosystem for EchoWave's word-synced lyrics integration. EchoWave calls the documented LyricsPlus v2 API and adapts its response format (including word/syllable timestamps) in its own Kotlin implementation; it does not bundle YouLyPlus extension code. EchoWave also queries Binimum's LyricsPlus catalog and validates its hosted TTML URL before parsing. LyricsPlus aggregates third-party lyrics services; the service and its community mirrors remain independently operated.
- [LRCLIB](https://lrclib.net) is used as an additional lyrics source and fallback when LyricsPlus has no usable result. EchoWave's client implementation is its own Kotlin code.

## Dependencies

The `innertubex` coordinate appears in the version catalog as a reference, but its dependency was reverted and is commented out in `app/build.gradle.kts`. It is not packaged in EchoWave. No `innertubex` source code has been copied into this project.

Direct runtime dependencies declared in `app/build.gradle.kts` include AndroidX Core, Activity, Lifecycle, Compose Material/UI, Navigation, Room, DataStore, Media3, Coil, Retrofit, OkHttp, Kotlinx Serialization, and Kotlin Coroutines. KSP and the Room compiler are build-time dependencies. JUnit, AndroidX Test, Espresso, and Compose UI Test are test dependencies. Exact declared versions are in `gradle/libs.versions.toml`; transitive dependencies are resolved by Gradle and can change with dependency updates. These dependencies are distributed under their respective licenses. This is a direct-dependency summary, not a complete generated transitive SBOM or license report; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

The app's YouTube/YouTube Music InnerTube integration is unofficial and is not an endorsement or statement of authorization. YouTube's [Developer Policies](https://developers.google.com/youtube/terms/developer-policies) prohibit undocumented API use without express permission and prohibit background audio playback for API Clients. EchoWave's current integration and background playback create an unresolved policy issue documented in [docs/INNER_TUBE_NOTES.md](docs/INNER_TUBE_NOTES.md). This note is included so source users can evaluate the issue; it does not establish legal applicability or permission.

## Trademarks and service names

YouTube, YouTube Music, Google, Spotify, Apple, and other product names are trademarks of their respective owners. EchoWave is an independent project and is not endorsed by or affiliated with those services. EchoWave does not host music or other provider media.
