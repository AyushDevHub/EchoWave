# EchoWave — Stitch design brief

Use this document as the product and interaction brief when designing EchoWave in Google Stitch. Design a real music product with a clear identity, not a generic “AI music app” concept.

## Product context

EchoWave is a mobile-first music player and discovery app. People search for a song, an artist, or a film soundtrack; browse albums; play a track or a whole collection; manage favorites, history, and playlists; read synced lyrics; and control what plays next.

The current product already has a recognizable direction: near-black listening surfaces, expressive editorial headings, high-visibility album artwork, soft rounded controls, and a compact mini-player above bottom navigation. Keep that recognizable EchoWave character while making the hierarchy and transitions feel considered and coherent.

The repository currently contains a native Android app rather than a website frontend. Design the primary screens for a phone first. Keep layouts adaptable to a wider browser or tablet without turning the phone design into a shrunken desktop dashboard.

## Design intent

- Make music artwork and music titles do most of the visual work.
- Make search, playback, and the next useful action obvious on the first glance.
- Give an album a proper destination: artwork, title, artist, Play all, and a visible, scrollable track list.
- Keep browsing calm and content-led. Use cards when they help recognition; do not put every row inside a card.
- Preserve useful information density for music lists. A listener should be able to scan several tracks without excessive scrolling.
- Make the interface feel alive through transitions tied to navigation and playback, not through constant decorative motion.
- Keep controls legible over artwork and preserve comfortable touch targets.

## EchoWave identity and visual material

Do not prescribe a new set of hex colors. Do not invent a trendy purple-to-blue gradient, neon glow, glass blur on every surface, or a palette that looks auto-generated.

Use the app’s selected appearance/theme as the source of interface colors. Let the current artwork contribute a restrained, temporary atmospheric tint on the full player where the app already supports that treatment. Keep text and controls readable if the artwork is bright, dark, or visually busy. Artwork colors should never recolor every screen or make navigation unstable.

Use supplied EchoWave branding and app assets where available. Use real music-cover imagery or believable editorial placeholders in mockups; never make up a fake EchoWave logo. Treat album covers as square source artwork and crop consistently. Keep typography expressive but disciplined: large display type belongs to page titles or the active track, while artist, album, duration, and metadata stay quieter.

Surface treatment should come from hierarchy, spacing, and artwork. Prefer a few intentional tonal layers and hairline separators over stacks of outlined cards, floating pills, and glass panels. Use rounded shapes consistently, with stronger rounding for artwork/player surfaces and lighter rounding for controls.

## Navigation model

Phone navigation has four persistent destinations:

1. **Home** — personal listening, recent items, favorites, and discovery shelves.
2. **Search** — query field, filters, and grouped results.
3. **Library** — favorites, history, playlists, and saved music.
4. **Settings** — appearance, playback, and app preferences.

Keep the mini-player visible above navigation while a track is selected. It shows small artwork, title, artist, play/pause, and a clear tap target to open the full player. Do not let the mini-player cover scrollable content or system navigation.

Album, artist, playlist, lyrics, queue, and full-player views are destinations or focused overlays that return naturally to the prior context. Back should preserve the search query, selected result filter, and scroll position.

## Screen requirements

### Home

- Start with a personal, understated greeting or “Welcome to EchoWave” heading, not a marketing hero.
- Show the most useful listening continuation first: recently played, favorites, or the current queue.
- Use horizontally browsable shelves for albums and moods; keep track lists easy to scan.
- Give each shelf a clear title and a lightweight “See all” action.
- Avoid repeating the same cover and title in several adjacent shelves when there is better content available.
- Include clear loading, empty, and error treatments that use the same layout rather than a blank screen.

### Search and results

- Keep the search field prominent and persistent while results load and scroll.
- Provide compact, horizontally scrollable result filters: All, Top result, Songs, Albums, Artists, and other supported types.
- In **All**, make the strongest playable match easy to spot, then show songs before less directly useful result types.
- For a movie query such as “Dhadkan” or “Rab Ne Bana Di Jodi”, surface original soundtrack songs and the relevant soundtrack album ahead of unrelated videos, remixes, lyric uploads, and exact-title noise.
- Clearly distinguish a song from a video, album, artist, and playlist through metadata and affordances. Do not make video thumbnails dominate a music search.
- A song row includes cover, title, artist, and an optional overflow action. Keep row height compact and tap targets generous.
- Retain the typed query and filters when opening a result and returning.
- Show a calm skeleton state while loading, helpful no-results guidance, and a retry action on failure.

### Album detail

- Use the album artwork, title, and artist to establish identity without consuming the whole first viewport.
- Keep **Play all** visible near the album heading.
- Show a track count and a full, scrollable, ordered list of songs. Each row shows title and artist; include duration when available.
- Tapping a row starts playback at that position with the remaining album tracks in the queue. Play all starts at the first track.
- Loading, empty, and error states must be explicit. Provide a retry action after an error.
- Preserve album and search context when navigating back.

### Full player

- Make the active artwork the focal point, with title and artist immediately legible.
- Keep transport controls, seek position, elapsed/remaining time, and playback state easy to reach.
- Expose favorite, lyrics, queue, shuffle, and repeat controls without crowding the primary play/pause action.
- Use artwork-derived atmosphere sparingly and preserve contrast over every cover.
- Keep the player useful with no lyrics, loading lyrics, or playback errors; do not leave large blank regions.

### Queue / Up next

- Clearly distinguish the currently playing track from upcoming tracks.
- Show artwork, title, and artist for each item. Make selection behavior obvious.
- Keep queue order stable and provide understandable empty/loading feedback if recommendations are unavailable.

### Library and settings

- Library should make Favorites, History, and Playlists easy to reach, with familiar rows and visible counts where useful.
- Playlist detail should show its tracks and a Play all action, following the album-detail behavior.
- Settings should be grouped and readable. Theme and player style previews should show the actual app treatment rather than abstract color swatches alone.

## Motion and transitions

Use purposeful, short motion. Motion should confirm an action, maintain spatial continuity, or communicate playback state. Keep the design usable when system reduced-motion preferences are enabled.

- **Screen navigation:** fade and move content a short distance (roughly 12–20 dp) over about 200–280 ms. Avoid dramatic page flips or long parallax.
- **Search results:** keep the search field and filter row anchored. Fade/translate result sections in lightly after loading; do not animate each row with a cascading delay.
- **Album open:** expand or crossfade the chosen cover into the album header while the title and track list enter with a restrained vertical fade. Back navigation reverses the transition where the platform allows it.
- **Mini-player to full player:** expand the mini-player artwork and surface into the full-player composition. Keep the active song identity visually continuous.
- **Play/pause:** use a quick icon morph or crossfade with a subtle tactile response. A small progress indicator may animate; do not pulse the entire screen.
- **Lyrics:** crossfade the active line and move it gently into focus as playback advances. Keep adjacent lines visible and avoid karaoke effects that reduce readability.
- **Queue and menus:** use a standard bottom-sheet slide with a soft dim layer. Dismiss by swipe, back, or tapping outside.
- **Artwork loading:** crossfade the placeholder into the loaded cover. Do not use shimmer indefinitely or allow the layout to jump when the image arrives.
- **Theme change:** transition surface and text roles together briefly; preserve layout and content position.
- **Reduced motion:** replace spatial movement with a short fade and disable breathing, pulsing, or continuously moving decoration.

Use platform-standard easing and durations rather than assigning a different bespoke motion to every component. No perpetual floating notes, bouncing equalizers outside active playback, cursor-following effects, or decorative animated gradients.

## Responsive behavior and accessibility

- Prioritize the phone layout and thumb reach. Keep primary actions in comfortable reach and use at least platform-standard touch targets.
- On wider layouts, use available width for artwork/list balance or a side panel; preserve the same information hierarchy and navigation destinations.
- Respect font scaling, display cutouts, system bars, keyboard insets, and reduced motion.
- Do not truncate essential track identity when a second line or overflow menu can preserve it.
- Use clear selected, playing, focused, disabled, buffering, and error states; never communicate state through color alone.
- Provide accessibility labels for icon-only actions and meaningful descriptions for artwork.

## Stitch deliverable

Generate a coherent set of connected screens, not unrelated mockups:

1. Home with mini-player and discovery shelves.
2. Search results for a movie soundtrack query, including filters, a soundtrack album, and original songs near the top.
3. Album detail with a multi-song track list and Play all.
4. Full player with artwork, transport, lyrics entry, and queue entry.
5. Queue / Up next.
6. Library and a playlist detail.
7. Settings with appearance and player-style choices.

Show at least one loading state, one empty state, and one failure-with-retry state among the relevant screens. Connect search → album → track playback → full player → queue → back to the prior context. Keep the composition implementable with native Android components and responsive web equivalents; do not invent functionality absent from EchoWave.

## Reference direction

Use these products as references for selected interaction ideas, not as templates to clone:

- **Apple Music:** editorial album presentation, artwork-led player, and a clear distinction between listening destinations and the active player.
- **Echo Music:** feature-rich listening flows, practical library organization, and visible lyrics/player affordances. [Repository and screenshots](https://github.com/EchoMusicApp/Echo-Music)
- **Namida:** flexible music browsing and dense-but-readable list handling across music and video. [Repository](https://github.com/namidaco/namida)
- **LastWave Native:** restrained use of expressive motion and contextual playback surfaces. [Repository](https://github.com/Clash-Projects/LastWave-native)

Do not reuse their logos, screenshots, artwork, exact layouts, names, or proprietary visual assets. EchoWave should remain recognizable as EchoWave through its current brand, existing appearance settings, and artwork-led listening experience.

## Paste-ready Stitch prompt

> Design EchoWave, a polished mobile-first music player and discovery app. Follow `design.md` as the source of truth. Keep EchoWave’s existing near-black listening character, expressive editorial headings, real album artwork, rounded controls, mini-player, and four destinations: Home, Search, Library, Settings. Do not invent a new fixed hex palette, neon purple/blue gradient, generic AI aesthetic, fake logo, or decorative glass everywhere. Derive interface colors from the app’s existing theme and use cover-art atmosphere only in the full player, with readable contrast. Create connected Home, movie-soundtrack Search results, Album detail with all songs and Play all, Full player, Queue, Library/Playlist, and Settings screens. Search for “Dhadkan” should make the original film songs and relevant soundtrack album easy to find above unrelated video results. Make album rows open a real multi-track detail page; tapping any song plays from that position and preserves the rest of the album in the queue. Keep screens compact, scannable, thumb-friendly, and accessible. Use short, purposeful transitions: subtle screen fade/slide, cover continuity from search to album to player, mini-player expansion, standard queue sheet, and gentle active-lyrics changes. Include loading, empty, and retryable error states. Respect reduced motion, preserve query/scroll context on back, and keep the same hierarchy on wider screens. Make it look like a finished EchoWave product, not a collection of unrelated generated mockups.
