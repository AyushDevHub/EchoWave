# Developer setup

1. Install Android Studio with the Android SDK required by `compileSdk` and the project JDK (see Gradle configuration).
2. Clone the repository and open it in Android Studio, or use the wrapper from the repository root.
3. Keep `local.properties` and any signing credentials out of Git. No private provider credential is required by the documented local build.
4. Run `./gradlew assembleDebug` (PowerShell: `\.\gradlew.bat assembleDebug`).
5. Run `./gradlew test` and `./gradlew lint` before proposing changes. The current playback/network flow depends on external providers, so tests should use the existing fakes where possible.
6. Install with `./gradlew installDebug` on a connected device. Confirm notification permission/device behavior and network availability when testing playback.

Read [ARCHITECTURE.md](../ARCHITECTURE.md) and [CREDITS.md](../CREDITS.md) before editing source/resolver code. Do not place API credentials in the APK, logs, or source control. Production release signing is not configured in this checkout.
