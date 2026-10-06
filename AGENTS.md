# AGENTS

Build agent for EchoWave (GPL-3.0, steady-clean).

Commands:
- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew lint`

Scope rules:
- P0 before P1 before P2. Never cut playback reliability for features.
- UI observes `playback/PlaybackController.kt:1` state. No direct ExoPlayer in UI.
- New source? Implement `domain/source/MusicSource.kt`, bind in `AppContainer.kt`.
- New resolver? Implement `domain/source/StreamResolver.kt`, append to chain in `AppContainer.kt`.
- No AI calls from playback/data. AI only in `features/ai/`.
- Every user flow needs loading/empty/error states.
- Attribution required: update CREDITS.md when porting donor code.
