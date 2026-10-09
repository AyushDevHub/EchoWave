# Privacy and Data Inventory

**Last reviewed:** 2026-10-09
**Status:** repository-level implementation inventory. This is not a substitute for a legally reviewed public privacy policy or a Google Play Data Safety declaration.

## Data handled by EchoWave

Based on the current app source:

| Data | Purpose | Storage / handling |
| --- | --- | --- |
| Display name, greeting preference, appearance and theme | Personalize the interface | Android DataStore on device |
| Music preference text | Tailor local discovery requests/suggestions | Android DataStore; may be used in provider search requests |
| Recent search terms (up to 10) | Show recent searches | Android DataStore; user can clear from Search |
| Favorites, playlists, listening history | Provide local library features | Room database on device |
| Playback starts, completions, skips, repeats | Build an on-device listening taste profile and session recommendations | Raw listening events are stored in Room for up to 12 months; a decayed aggregate profile is stored in DataStore. Collection is enabled by default; there is currently no in-app DNA collection toggle. |
| Radio candidate metadata | Build the next-song queue and avoid repeats | Fetched from the current unofficial YouTube Music `/next` integration and held in an in-memory cache for up to two hours; local play history/profile is not sent with the request |
| Visitor identifier returned by InnerTube | Maintain provider request context | SharedPreferences on device; sent to YouTube endpoints |
| Current queue and playback position/state | Operate the media session | Playback service/player while in use; queue persistence behavior may vary by lifecycle |
| Fetched stream bytes | Avoid downloading the same portions again, including when seeking | Media3 disk LRU cache on device, maximum 500 MiB; entries may be evicted |

The app has no EchoWave account service, and no analytics or crash-reporting SDK is declared in the app module. Android backup is enabled. The provided backup-rule files contain no custom inclusion/exclusion policy, so do not promise that stored preferences or library data are excluded from device/cloud backup. Users can clear app storage or uninstall to reset local state; uninstall behavior depends on Android backup/restore settings.

## Network services and data sent

The app makes requests to undocumented YouTube/YouTube Music InnerTube endpoints for search, discovery, metadata, seed-based radio candidates, visitor state, and playback stream resolution. Search terms are sent for search; the selected track's provider ID is sent to request radio candidates; the resulting stream URL is used for bounded media byte-range requests. Artwork is loaded from the remote URL in provider metadata. These services receive the relevant query or track identifier, IP address, and normal network/request metadata. EchoWave does not send its local listening events, favorites, DNA profile, or session history to YouTube as recommendation features. The stream URL and its parameters are sensitive and are not logged by EchoWave.

When the Home screen is opened, the update checker may query GitHub's public latest-release endpoint (at most once every six hours per app process, unless the user taps Retry after an error). The request identifies the app version in its User-Agent; GitHub also receives the device IP address and normal request metadata. EchoWave reads the release tag and APK asset name, does not send account or library data, and does not automatically download or install the APK. Tapping an available-update chip opens the GitHub release page.

Lyrics lookup can contact LRCLIB and several independently operated LyricsPlus mirrors/catalog endpoints using track title and artist metadata. Those services also receive IP address and normal network/request metadata. Their handling and retention practices are outside EchoWave's control and have not been independently verified. The current YouTube integration is unofficial, uses undocumented endpoints, and has unresolved provider-policy risk; see [InnerTube notes](INNER_TUBE_NOTES.md).

EchoWave does not operate a backend that collects these requests. Network providers may log or process them according to their own terms and policies. The current YouTube source is unofficial and has unresolved provider-policy risk; see [InnerTube notes](INNER_TUBE_NOTES.md).

## User controls

- Clear recent searches using the Search screen's clear action.
- Remove favorites, playlists, or history items through the Library UI where supported.
- Change or disable the greeting, name, theme, and taste preferences in Settings.
- Clear local taste events/profile and library data by clearing app storage in Android settings. There is currently no separate in-app control to disable or clear DNA data. This does not disable provider requests.
- Clear all app data through Android system settings to reset local app storage.

## Before app-store distribution

The project must publish a user-facing policy with a responsible contact method, verify exact backup and deletion behavior, review third-party SDK/service handling, and complete the relevant store privacy disclosures. The GitHub repository issue tracker is not a suitable place to post a user's private data or deletion request.
