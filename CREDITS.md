# Credits & Licenses

EchoWave is GPL-3.0 (see LICENSE). GitHub APK releases include source.

Donors / inspiration — appreciation + license compliance:

- team-spotube/spotube — BSD-4-Clause. Concept reference only (plugin-style
  source abstraction, Flutter/Dart so no direct Kotlin reuse). Preserve BSD
  attribution if any logic is ported.
- EchoMusicApp/Echo-Music — GPL-3.0. Primary donor for `innertube/` search +
  player endpoints + decipher, `playback/` service patterns, BravePipe backup
  engine mention, LRCLIB lyrics reference. Any ported files keep their headers
  and remain GPL-3.0.
- Clash-Projects/LastWave-native — GPL-3.0. Donor for Kotlin + Compose +
  Media3 + MVVM/Clean + Room/DataStore/Hilt structure, queue/shuffle/repeat,
  scrobbler/discovery concepts (not MVP).
- NewPipe Extractor (Team NewPipe, GPL-3.0) — optional fallback resolver pattern.
- BravePipe project — backup decipher/playback concept credited by Echo-Music.
  NOT used anywhere in EchoWave (donor commented it out too).

## PO-token + stream pipeline (Echo-Music approach, GPL-3.0)

- `app/src/main/assets/po_token.html` vendored verbatim from Echo-Music
  (BotGuard WebView bootstrap JS), plus attribution header. Rest is a
  clean-room port of their policy, not their code:
  `PoTokenCache`/`WebViewPoTokenProvider` (session pot, 8 s timeout,
  expiry margin, prewarm), `JsCodec` (challenge/integrity/u8 codecs),
  `PlayerClient` (c/cver fetch dressing + range sizes).
- Stream extractor/cipher catalog reused as a dependency, not ported:
  `com.github.MetrolistGroup.innertubex:innertubex-android:v0.7.0`
  (JitPack; upstream license applies — GPL-3.0 family).
- NewPipeExtractor + PipePipe extractor lineage acknowledged via the above.

YouTube / YouTube Music are trademarks of Google. EchoWave hosts no media,
provides no catalog itself, and is not affiliated with Google, Spotify, Apple,
or Last.fm. Source-layer breakage is expected; that is why `MusicSource` /
`StreamResolver` are replaceable.
