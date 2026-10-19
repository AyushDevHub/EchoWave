# Privacy and Data Inventory

**Last reviewed:** 2026-10-20
**Status:** repository-level implementation inventory. This is not a substitute for a legally reviewed public privacy policy or a Google Play Data Safety declaration.

## Data handled by EchoWave

Based on the current app source:

| Data | Purpose | Storage / handling |
| --- | --- | --- |
| Display name, greeting preference, appearance and theme | Personalize the interface | Android DataStore on device |
| Music preference text | Tailor local discovery requests/suggestions | Android DataStore; may be used in provider search requests |
| Recent search terms (up to 10) | Show recent searches | Android DataStore; user can clear from Search |
| Favorites, playlists, listening history | Provide local library features | Room database on device |
| Playback starts, completions, skips, repeats | Build an on-device listening taste profile and session recommendations | Raw listening events are stored in Room for up to 12 months; a decayed aggregate profile is stored in DataStore. In Settings, users can stop collection and DNA-based personalization, or reset the stored events and profile. Data stays on the device. |
| Listening Summary | Show listening time, qualifying plays, completions, and local rankings | Calculated on-device from retained playback outcomes. Summary periods use the event finish time; incomplete sessions without a saved outcome are not counted. The optional time-of-day breakdown is hidden by default and uses the hour already stored in playback events. |
| Radio candidate metadata | Build the next-song queue and avoid repeats | Fetched from the current unofficial YouTube Music `/next` integration and held in an in-memory cache for up to two hours; local play history/profile is not sent with the request |
| Visitor identifier returned by InnerTube | Maintain provider request context | SharedPreferences on device; sent to YouTube endpoints |
| Current queue and playback position/state | Operate the media session | Playback service/player while in use; queue persistence behavior may vary by lifecycle |
| Fetched stream bytes | Avoid downloading the same portions again, including when seeking | Media3 disk LRU cache on device, maximum 500 MiB; entries may be evicted |
| Audio files explicitly selected in the Library's On device tab | Provide offline playback for user-provided audio | Copied to the app-specific `Music/EchoWave/Downloads` directory (internal app-files fallback); audio bytes are not stored in Room and this feature does not upload them. Android removes app-specific files on uninstall. Backup/restore follows Android backup configuration. |
| Local audio filenames and embedded tags | Display and identify imported files | Read locally through the system picker and media metadata reader; no broad shared-storage permission is requested |

The app has no EchoWave account service, and no analytics or crash-reporting SDK is declared in the app module. Android backup is enabled. The provided backup-rule files contain no custom inclusion/exclusion policy, so do not promise that stored preferences or library data are excluded from device/cloud backup. Users can clear app storage or uninstall to reset local state; uninstall behavior depends on Android backup/restore settings.

## Network services and data sent

The app makes requests to undocumented YouTube/YouTube Music InnerTube endpoints for search, discovery, metadata, seed-based radio candidates, visitor state, and playback stream resolution. Search terms are sent for search; the selected track's provider ID is sent to request radio candidates; the resulting stream URL is used for bounded media byte-range requests. Artwork is loaded from the remote URL in provider metadata. These services receive the relevant query or track identifier, IP address, and normal network/request metadata. EchoWave does not send its local listening events, favorites, DNA profile, or session history to YouTube as recommendation features. The stream URL and its parameters are sensitive and are not logged by EchoWave.

When the Home screen is opened, the update checker may query GitHub's public latest-release endpoint (at most once every six hours per app process, unless the user taps Retry after an error). The request identifies the app version in its User-Agent; GitHub also receives the device IP address and normal request metadata. EchoWave reads the release tag and APK asset name, does not send account or library data, and does not automatically download or install the APK. Tapping an available-update chip opens the GitHub release page. If a newer release is found, the app may post a local notification; on Android 13 and newer, Android requires notification permission. This check runs only when the app is opened; it is not remote push.

The About screen can query GitHub for the latest APK asset's public download count. This is GitHub's aggregate asset counter, not an app install or active-user counter. The request sends no device ID or usage event to EchoWave maintainers; GitHub still receives the IP address and normal request metadata described above.

The Settings feedback link opens GitHub Issues in the user's browser. Submitting a report is optional and is handled by GitHub under its own privacy practices. EchoWave does not attach device identifiers, listening history, or app usage to the issue link. Users should avoid including private or identifying information in a public issue.

Lyrics lookup can contact LRCLIB and several independently operated LyricsPlus mirrors/catalog endpoints using track title and artist metadata. Those services also receive IP address and normal network/request metadata. Their handling and retention practices are outside EchoWave's control and have not been independently verified. The current YouTube integration is unofficial, uses undocumented endpoints, and has unresolved provider-policy risk; see [InnerTube notes](INNER_TUBE_NOTES.md).

EchoWave does not operate a backend that collects these requests. Network providers may log or process them according to their own terms and policies. The current YouTube source is unofficial and has unresolved provider-policy risk; see [InnerTube notes](INNER_TUBE_NOTES.md).

## User controls

- Clear recent searches using the Search screen's clear action.
- Remove favorites, playlists, or history items through the Library UI where supported.
- Change or disable the greeting, name, theme, and taste preferences in Settings.
- Disable Music DNA collection and personalization in Settings. Existing events and the profile remain locally stored until the user selects **Reset Music DNA**; reset deletes those events and the profile without deleting favorites or playlists. Re-enabling Music DNA makes the retained profile available again, or starts fresh after a reset. This does not disable provider requests.
- Enable or hide Listening Summary's time-of-day breakdown. The setting controls display; the local hour is already part of playback events used by Music DNA.
- Clear listening history from Listening Summary. This deletes the shared local playback events and resets Music DNA; favorites and playlists are preserved.
- Clear all app data through Android system settings to reset local app storage.
- Import and remove user-authorized audio in Library → On device. Removing a row deletes its app-owned copy. Import only audio you own or have permission to store; this is not a provider download feature.

## Before app-store distribution

The project must publish a user-facing policy with a responsible contact method, verify exact backup and deletion behavior, review third-party SDK/service handling, and complete the relevant store privacy disclosures. The GitHub repository issue tracker is not a suitable place to post a user's private data or deletion request.
