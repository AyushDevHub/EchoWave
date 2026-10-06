# Privacy and Data Inventory

**Last reviewed:** 2026-10-07
**Status:** repository-level implementation inventory. This is not a substitute for a legally reviewed public privacy policy or a Google Play Data Safety declaration.

## Data handled by EchoWave

Based on the current app source:

| Data | Purpose | Storage / handling |
| --- | --- | --- |
| Display name, greeting preference, appearance and theme | Personalize the interface | Android DataStore on device |
| Music preference text | Tailor local discovery requests/suggestions | Android DataStore; may be used in provider search requests |
| Recent search terms (up to 10) | Show recent searches | Android DataStore; user can clear from Search |
| Favorites, playlists, listening history | Provide local library features | Room database on device |
| Visitor identifier returned by InnerTube | Maintain provider request context | SharedPreferences on device; sent to YouTube endpoints |
| Current queue and playback position/state | Operate the media session | Playback service/player while in use; persistence behavior may vary by lifecycle |

The app has no EchoWave account service, and no analytics or crash-reporting SDK is declared in the app module. Android backup is enabled. The provided backup-rule files contain no custom inclusion/exclusion policy, so do not promise that stored preferences or library data are excluded from device/cloud backup. Users can clear app storage or uninstall to reset local state; uninstall behavior depends on Android backup/restore settings.

## Network services and data sent

The app makes requests to YouTube/YouTube Music InnerTube endpoints for search, discovery, metadata, and playback; it loads artwork referenced by metadata. Lyrics lookup can contact LRCLIB and several independently operated LyricsPlus mirrors/catalog endpoints. Those services receive request data such as search terms or track metadata, IP address, and normal network/request metadata. Their own handling and retention practices are outside EchoWave's control and have not been independently verified.

EchoWave does not operate a backend that collects these requests. Network providers may log or process them according to their own terms and policies. The current YouTube source is unofficial and has unresolved provider-policy risk; see [InnerTube notes](INNER_TUBE_NOTES.md).

## User controls

- Clear recent searches using the Search screen's clear action.
- Remove favorites, playlists, or history items through the Library UI where supported.
- Change or disable the greeting, name, theme, and taste preferences in Settings.
- Clear all app data through Android system settings to reset local app storage.

## Before app-store distribution

The project must publish a user-facing policy with a responsible contact method, verify exact backup and deletion behavior, review third-party SDK/service handling, and complete the relevant store privacy disclosures. The GitHub repository issue tracker is not a suitable place to post a user's private data or deletion request.
