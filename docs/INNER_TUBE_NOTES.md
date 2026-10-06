# YouTube / InnerTube Integration Notes

**Status:** unofficial integration; no authorization from Google or YouTube is documented.

## What the app does

EchoWave currently sends requests to YouTube Music/YouTube InnerTube endpoints for search, discovery, metadata, visitor state, BotGuard/PO-token handling, stream resolution, and media delivery. The integration is implemented in `data/remote/innertube/`, `data/remote/potoken/`, and `data/playback/`. It is not a supported public API contract. Endpoint shapes, access controls, throttling, and availability can change without notice.

Some client identifiers in the app are public identifiers, not private user credentials. They are visible in the source, APK, and Git history. Do not treat that visibility as permission. Never commit account cookies, passwords, OAuth refresh tokens, private backend credentials, or signed stream URLs.

## Terms and policy questions

YouTube's [Terms of Service](https://www.youtube.com/t/terms) restrict automated access to the Service using means such as robots, botnets, or scrapers, subject to listed exceptions such as prior written permission or applicable law. The [API Services Developer Policies](https://developers.google.com/youtube/terms/developer-policies) say API Clients must not use undocumented APIs without express permission and restrict separating audio from video and background playback when using YouTube API Services.

EchoWave uses undocumented endpoints and extracts audio for background music playback. The precise application of each document to this implementation should be assessed by qualified counsel or resolved directly with YouTube; this repository has no evidence of written permission. The safest project decision is to obtain written authorization or replace this source with a provider and integration that expressly permit the intended behavior. A disclaimer, GPL license, API key, or another app's similar behavior does not create authorization.

## User and maintainer guidance

- Expect provider-side breakage, throttling, access restrictions, or request blocking.
- Do not evade blocks or restrictions by rotating keys, accounts, cookies, proxies, or client identities.
- Do not claim that YouTube endorses EchoWave or that the integration is authorized.
- If contacted by a rights holder or provider, preserve the notice, review the stated issue, and obtain qualified advice before responding.
- Do not use this code to host, redistribute, or claim ownership of third-party music.

The project risk and possible platform outcomes are described in [RISK_REGISTER.md](RISK_REGISTER.md). The app's playback architecture is documented in [ARCHITECTURE.md](../ARCHITECTURE.md).
