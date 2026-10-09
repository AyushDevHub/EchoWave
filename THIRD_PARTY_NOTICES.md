# Third-Party Notices

EchoWave is licensed under GPL-3.0. See [LICENSE](LICENSE), [NOTICE](NOTICE), and [CREDITS.md](CREDITS.md) for donor-code and reference attributions.

## Application dependencies

The direct dependencies and exact versions are declared in [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`app/build.gradle.kts`](app/build.gradle.kts). Direct application dependencies include:

- AndroidX Core, Activity, Lifecycle, Compose UI/Material, Navigation, Room, DataStore, and Media3.
- Kotlin, Kotlinx Serialization, and Kotlin Coroutines.
- Coil, Retrofit, and OkHttp.
- KSP and Room compiler for code generation.
- JUnit, AndroidX Test, Espresso, and Compose UI testing libraries for tests.

These projects carry their own license terms and notices. Consult the exact resolved versions in Gradle's dependency report and the license files supplied by each artifact. This repository does not yet contain a generated complete transitive SBOM or license-text bundle. Generate and review both for every store/distribution release.

## Included donor code and service integrations

- `app/src/main/assets/po_token.html` is vendored from [Echo-Music](https://github.com/EchoMusicApp/Echo-Music); its source attribution is retained. EchoWave also adapts portions of the donor's InnerTube/PO-token stream pipeline. See [CREDITS.md](CREDITS.md).
- Spotube, NewPipe Extractor, and BravePipe are reference-only or placeholder mentions; their implementation code is not included as an active dependency. LastWave-native text-matching (`core/common/TextMatch.kt`) is ported code under GPL-3.0; see [CREDITS.md](CREDITS.md).
- LyricsPlus-compatible endpoints and LRCLIB are network services, not bundled source code. Their operators' terms and data practices are independent.
- Search, discovery, radio candidate metadata, stream resolution, and playback currently use undocumented YouTube/YouTube Music InnerTube flows. These network services are not bundled libraries, and this notice does not imply provider authorization. EchoWave's local recommendation scorer is original project code; no third-party recommendation model/library was added for this feature. See [InnerTube notes](docs/INNER_TUBE_NOTES.md) and [privacy inventory](docs/PRIVACY.md).
- Bundled fonts in `app/src/main/res/font/`:
  - **Outfit** (`outfit.ttf`): Copyright (c) 2021 The Outfit Project Authors. Licensed under the SIL Open Font License, Version 1.1 (OFL-1.1).
  - **Plus Jakarta Sans** (`plus_jakarta_sans.ttf`): Copyright (c) 2020 The Plus Jakarta Sans Project Authors. Licensed under the SIL Open Font License, Version 1.1 (OFL-1.1).
- No music or remote artwork is bundled in the APK.

The `innertubex` coordinate in the version catalog is inactive and is not declared as an app dependency. Recheck the Gradle dependency graph when dependencies change.
