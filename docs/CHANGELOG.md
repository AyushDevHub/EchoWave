# Changelog

User-visible changes are listed here. Internal maintenance and exhaustive file changes are recorded in Git history.

## 1.0.2 (2026-10-20)

- Added a refresh action to the Up next queue. It keeps the current track and replaces only upcoming recommendations.
- Added a Settings shortcut to GitHub's feature request and feedback forms.
- Added the public latest-APK download count to About and a local update notification when a newer release is found while the app is open.
- Reorganized the README, documented the limits of download and active-use statistics, and added screenshots captured from the app.
- Fixed the stream probe's last-byte range, ignored stale player-ended callbacks after a new track selection, and stopped queue navigation from racing through multiple unavailable tracks.
- Added Settings controls to pause Music DNA collection and personalization or reset its local listening data.
- Added a local Listening Summary with today, week, month, and retained-history views, explicit play/completion rules, and opt-in time-of-day display.
- Corrected singular play-count wording in the summary.
- Added an On device Library tab that copies user-selected audio into EchoWave's app-specific files and routes it through Media3 for offline playback. This does not download provider catalog tracks.
- Added Echo This from Now Playing: review ranked candidates based on the current track and available on-device listening signals, then add selected tracks to the queue without replacing it. Music DNA influence now grows with supporting event volume.
- Added a Music DNA profile card to Listening Summary and per-candidate reasons in Echo This, based on the actual scoring signals used for ranking.
- Avoided showing a time-of-day-derived profile label when that optional display is off; candidates with unknown metadata no longer receive an unsupported fresh-match reason or bonus.
- Added a broader Bollywood artist picker with catalog artwork and progressive suggestions, including K.K. and Sukhwinder Singh.

Published as a GitHub APK release. The existing provider-authorization and content-rights risks remain unresolved and are disclosed in the [release notes](releases/1.0.2.md) and [risk register](RISK_REGISTER.md).

## 1.0.3 (2026-10-20)

- Changed the in-app update chip and local update notification to open the EchoWave download website, with simpler install instructions for users.
- Published as a signed GitHub APK release; the download page offers the latest APK directly.

## Unreleased

No unreleased user-facing changes.

## 1.0.1 (2026-10-10)

- Expanded discovery with typed search filters, album detail track lists, browse collections, and seed-based radio autoplay from a selected song.
- Added a private, on-device listening profile and recommendations informed by the seed, recent plays and skips, favorites, and library history. Listening events are stored locally; the profile and history are not sent with radio requests.
- Improved track-change lyrics state and lyric timing, adjusted seek rebuffer behavior, and expanded provider, privacy, dependency, and release documentation.
- Added direct attribution for the selected Echo-Music donor implementation areas and documented which recommendation work is EchoWave code.
- Removed three unused MP3 resources with undocumented provenance from the current source tree; earlier Git history still contains them.
- Added a compact Home-screen update chip that checks GitHub Releases and opens the release page when a newer APK is available.

Published as a GitHub APK release. Provider authorization and content-rights review remain unresolved; see [release status](RELEASE_PROCESS.md) and [release notes](releases/1.0.1.md).

## 1.0.0 — 2026-10-07

First public GitHub APK release.

- Refined the home, search, library, settings, and now-playing screens.
- Added selectable color themes, optional personalized greeting, and taste-led discovery suggestions.
- Added browse collections, recent search history, and persistent local favorites, playlists, and listening history.
- Improved the compact player progress ring, playback typography, and word-timed lyrics presentation.
- Added project setup, security, privacy, release, test, risk, support, and attribution documentation.
- Added GitHub issue forms, pull request template, and a tag-triggered APK release workflow.

See the [v1.0.0 release notes](releases/1.0.0.md) and [release page](https://github.com/AyushDevHub/EchoWave/releases/tag/v1.0.0). The unofficial InnerTube integration remains subject to unresolved provider-policy risk; see [RISK_REGISTER.md](RISK_REGISTER.md).
