# EchoWave privacy notes

**Status:** implementation inventory for review, not a published privacy policy or legal advice.

## Data stored on the device

- Display name, greeting toggle, theme, and music taste terms are stored in Android DataStore preferences.
- Recent searches are stored locally (up to 10 query strings).
- Favorites, playlists, and listening history are stored in the local Room database.
- Playback queue and current playback state are managed by the player/session while the app is running.

The app has no account/login flow and no analytics or crash-reporting SDK is declared in the app module. Android backup is enabled in the manifest with the sample backup configuration; cloud/device-transfer behavior must be decided and tested before making a “device-only” privacy claim.

## Network services

Search and stream resolution use YouTube Music/YouTube InnerTube endpoints. Discovery uses the same provider. Lyrics can query LyricsPlus mirrors and LRCLIB. Artwork URLs are loaded from provider metadata. Search terms, track IDs/metadata, network identifiers, and service requests may therefore be sent to those independently operated services. Provider practices and terms are separate from EchoWave.

EchoWave does not host music. This inventory does not establish that a provider permits the app's use or that a user's use is lawful in their region.

## User controls and data deletion

Recent search terms can be cleared from Search. Favorites and playlists can be changed in Library. The current UI does not provide a single “delete all local data” action; Android app-data clearing/uninstall is the available whole-app reset. Add an in-app reset and verify database/backup deletion before release if product policy requires it.

## Before publication

Publish an accurate policy covering provider requests, local history, backup behavior, retention/deletion, children's use, and contact details. Review SDK/dependency data practices and complete Google Play Data Safety using the final release build. Do not treat this implementation note as the public policy.
