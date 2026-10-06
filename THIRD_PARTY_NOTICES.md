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
- Spotube, LastWave-native, NewPipe Extractor, and BravePipe are reference-only or placeholder mentions; their implementation code is not included as an active dependency.
- LyricsPlus-compatible endpoints and LRCLIB are network services, not bundled source code. Their operators' terms and data practices are independent.
- No music or remote artwork is intended to be bundled in the APK. Verify licenses for any future bundled font, icon, image, or media asset.

The `innertubex` coordinate in the version catalog is inactive and is not declared as an app dependency. Recheck the Gradle dependency graph when dependencies change.
