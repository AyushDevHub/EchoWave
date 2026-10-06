# InnerTube integration notes

EchoWave currently uses unofficial YouTube Music/YouTube InnerTube endpoints for search, discovery, playback resolution, and stream metadata. These are not a stable public music API contract; response shapes, client behavior, rate limits, and availability can change without notice.

The app contains a public client API key used by the InnerTube request flow. It is not a private user credential or server secret, but it is visible in the APK and Git history. Confirm the key's provenance, ownership, restrictions, and allowed use before release. Never add account cookies, OAuth refresh tokens, private backend credentials, or user-specific tokens to source or logs.

Playback relies on the resolver chain registered in `AppContainer.kt`, request headers in the InnerTube client code, the PO-token provider, and bounded range data sources. Preserve this path when changing discovery or UI: screens observe `PlaybackController` and must not access ExoPlayer directly.

## Provider-policy release blocker

YouTube's current [API Services Developer Policies](https://developers.google.com/youtube/terms/developer-policies) state that API Clients must not use undocumented APIs without express permission (section III.E.4) and must not enable background audio playback (section III.E.6). EchoWave currently uses undocumented InnerTube endpoints and provides background audio playback. This is a concrete policy conflict for an API Client use case and a **public distribution blocker** until the integration is redesigned around permitted interfaces or the relevant permission and applicability are resolved. A disclaimer does not grant permission. This assessment is a release engineering flag, not legal advice; have the service terms reviewed for the actual deployment.

Before distribution, resolve the policy concern above and review applicable copyright rules for the exact use case. Technical reachability is not permission to use, cache, or redistribute content. Test provider errors, rate limits, expiry, 403/range regressions, and fallback exhaustion on a physical device.
