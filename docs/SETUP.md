# Developer Setup

## Requirements

- Android Studio with Android SDK Platform 37 and the required SDK build tools.
- JDK 17. The Android CI workflow uses Temurin 17.
- Git and network access for Gradle dependency resolution.

## Get the source

```powershell
git clone https://github.com/AyushDevHub/EchoWave.git
cd EchoWave
```

Open the project in Android Studio and let Gradle sync. Android Studio creates the machine-specific `local.properties` file; it is ignored by Git. Never add signing credentials to it or commit it.

## Build and run

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat installDebug
```

The debug APK is under `app/build/outputs/apk/debug/`. Playback and discovery require network access and currently depend on unofficial external provider endpoints; build success does not mean those services are available or that their use is authorized.

## Release build

Release signing is configured locally through `%USERPROFILE%\.android\echowave-release.properties` on Windows or `~/.android/echowave-release.properties` on Unix-like systems. That file points to the private release keystore. Use a secure backup, and never commit either file.

```powershell
.\gradlew.bat test lint assembleRelease bundleRelease
```

Without the local signing configuration, Gradle may produce unsigned release artifacts. Follow [the release process](RELEASE_PROCESS.md); do not publish an artifact until its signature, checksum, device behavior, content rights, and provider terms have been reviewed.

## Repository guidance

Read [architecture](../ARCHITECTURE.md), [contributing](../CONTRIBUTING.md), [security](../SECURITY.md), [privacy notes](PRIVACY.md), and [credits](../CREDITS.md) before making source or provider changes.
