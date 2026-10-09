# Changelog

User-visible changes are listed here. Internal maintenance and exhaustive file changes are recorded in Git history.

## Unreleased

- Search song taps now start seed-based radio autoplay instead of queueing the remaining search results ahead of recommendations.
- Recommendation ranking now uses seed style/language, recent skips and plays, the local taste profile, favorites, and recent library history; local taste data stays on device.
- Reduced the playback rebuffer threshold after seeks and documented YouTube/YouTube Music data flows, local recommendation signals, caching, and the unofficial provider status.

## 1.0.0 — 2026-10-07

First public GitHub APK release.

- Refined the home, search, library, settings, and now-playing screens.
- Added selectable color themes, optional personalized greeting, and taste-led discovery suggestions.
- Added browse collections, recent search history, and persistent local favorites, playlists, and listening history.
- Improved the compact player progress ring, playback typography, and word-timed lyrics presentation.
- Added project setup, security, privacy, release, test, risk, support, and attribution documentation.
- Added GitHub issue forms, pull request template, and a tag-triggered APK release workflow.

See the [v1.0.0 release notes](releases/1.0.0.md) and [release page](https://github.com/AyushDevHub/EchoWave/releases/tag/v1.0.0). The unofficial InnerTube integration remains subject to unresolved provider-policy risk; see [RISK_REGISTER.md](RISK_REGISTER.md).
