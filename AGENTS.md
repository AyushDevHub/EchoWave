# Repository agent instructions

These instructions apply to automated coding agents and maintainers working in EchoWave. The project is GPL-3.0 licensed; see `LICENSE` and `CREDITS.md`.

## Build and validation

Run commands from the repository root.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lint
```

On macOS/Linux, use the equivalent wrapper:

```sh
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

Do not claim a test, device scenario, security scan, or release check passed unless it was run. Review warnings instead of suppressing them without explanation.

## Engineering constraints

- Prioritize P0 playback reliability over P1 and P2 features. See `ARCHITECTURE.md`.
- UI observes `PlaybackController` state. UI code must not access ExoPlayer directly.
- Implement sources through `domain/source/MusicSource.kt` and resolvers through `domain/source/StreamResolver.kt`; bind them in `AppContainer.kt`.
- Keep AI integrations inside `features/ai/`; no AI calls from playback or data layers.
- Every new user flow needs loading, empty, and error states.
- Preserve privacy: do not log or commit credentials, cookies, PO tokens, signed URLs, request headers, or personal data.
- Review provider terms before adding or changing a source. Do not evade provider restrictions or represent undocumented access as authorized.
- Update attribution in `CREDITS.md` and `THIRD_PARTY_NOTICES.md` when porting code or adding dependencies/assets.
- Update user and developer documentation when behavior, setup, privacy, or release procedures change.
