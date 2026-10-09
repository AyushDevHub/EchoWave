package com.howdy.echowave.data.remote.innertube

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InnerTubeMappersTest {
    private fun json(raw: String) =
        innerTubeJson.parseToJsonElement(raw) as kotlinx.serialization.json.JsonObject

    @Test fun `parses search items and skips id-less ones`() {
        val root = json(
            """{"contents":{"sectionListRenderer":{"contents":[
              {"musicShelfRenderer":{"contents":[
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Blinding Lights"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"The Weeknd"},{"text":" • "},{"text":"After Hours"}]}}}
                  ],
                  "fixedColumns":[{"musicResponsiveListItemFixedColumnRenderer":{"text":{"runs":[{"text":"3:20"}]}}}],
                  "playlistItemData":{"videoId":"abc123"},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://small"},{"url":"http://big"}]}}}
                }},
                {"musicResponsiveListItemRenderer":{"flexColumns":[]}}
              ]}}
            ]}}}""",
        )
        val results = parseSearchResponse(root)
        assertEquals(1, results.songs.size)
        assertEquals("abc123", results.songs[0].id)
        assertEquals("Blinding Lights", results.songs[0].title)
        assertEquals("The Weeknd", results.songs[0].artist)
        assertEquals("After Hours", results.songs[0].album)
        assertEquals("http://big", results.songs[0].artworkUrl)
        assertEquals(200_000L, results.songs[0].durationMs)
        assertEquals(results.songs[0], (results.topResult as com.howdy.echowave.domain.model.SearchItem.Song).track)
    }

    @Test fun `picks highest-bitrate audio url`() {
        val root = json(
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"adaptiveFormats":[
              {"mimeType":"video/mp4","url":"http://v","bitrate":1000},
              {"mimeType":"audio/webm; codecs=opus","url":"http://lo","bitrate":64000},
              {"mimeType":"audio/mp4; codecs=mp4a","url":"http://hi","bitrate":128000}
            ]}}""",
        )
        val p = parsePlayerResponse("v1", root)
        assertTrue(p is PlayerParse.Playable)
        assertEquals("http://hi", (p as PlayerParse.Playable).info.url)
    }

    @Test fun `ciphered-only is reported not silent`() {        val root = json(
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"adaptiveFormats":[
              {"mimeType":"audio/webm","signatureCipher":"s=abc","bitrate":64000}
            ]}}""",
        )
        val p = parsePlayerResponse("v1", root)
        assertTrue(p is PlayerParse.Unplayable)
    }

    @Test fun `unplayable status surfaces reason`() {
        val root = json("""{"playabilityStatus":{"status":"LOGIN_REQUIRED","reason":"Age restricted"}}""")
        val p = parsePlayerResponse("v1", root)
        assertTrue(p is PlayerParse.Unplayable)
    }

    @Test fun `poToken passes through from serviceIntegrityDimensions`() {
        val root = json(
            """{"playabilityStatus":{"status":"OK"},
                "serviceIntegrityDimensions":{"poToken":"TOKEN123"},
                "streamingData":{"adaptiveFormats":[
                  {"mimeType":"audio/webm","url":"http://a","bitrate":64000}
                ]}}""",
        )
        val p = parsePlayerResponse("v1", root)
        assertTrue(p is PlayerParse.Playable)
        assertEquals("TOKEN123", (p as PlayerParse.Playable).info.poToken)
    }

    @Test fun `no poToken stays null`() {
        val root = json(
            """{"playabilityStatus":{"status":"OK"},"streamingData":{"adaptiveFormats":[
              {"mimeType":"audio/webm","url":"http://a","bitrate":64000}
            ]}}""",
        )
        val p = parsePlayerResponse("v1", root)
        assertEquals(null, (p as PlayerParse.Playable).info.poToken)
    }

    @Test fun `extractVisitorData reads responseContext`() {
        val root = innerTubeJson.parseToJsonElement(
            """{"responseContext":{"visitorData":"VIS123"}}""",
        ) as kotlinx.serialization.json.JsonObject
        assertEquals("VIS123", extractVisitorData(root))
        val bare = innerTubeJson.parseToJsonElement("""{}""") as kotlinx.serialization.json.JsonObject
        assertEquals(null, extractVisitorData(bare))
    }

    @Test fun `visitor store keeps latest id`() {
        val store = com.howdy.echowave.data.remote.innertube.VisitorStore.inMemory()
        assertEquals(null, store.current())
        assertEquals(true, store.offer("A"))
        assertEquals(false, store.offer("A"))
        assertEquals(false, store.offer(null))
        assertEquals("A", store.current())
        assertEquals(true, store.offer("B"))
        assertEquals("B", store.current())
    }

    @Test fun `search body carries visitorData when known`() {        val with = com.howdy.echowave.data.remote.innertube.searchBody("q", "VIS")
        val client = with["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("VIS", client["visitorData"]!!.jsonPrimitive.content)
        val without = com.howdy.echowave.data.remote.innertube.searchBody("q", null)
        assertEquals(
            false,
            without["context"]!!.jsonObject["client"]!!.jsonObject.containsKey("visitorData"),
        )
    }

    @Test fun `search body carries filter params only when given`() {
        val filtered = com.howdy.echowave.data.remote.innertube.searchBody(
            "q", null, com.howdy.echowave.domain.model.SearchFilter.SONGS.params,
        )
        assertEquals(
            com.howdy.echowave.domain.model.SearchFilter.SONGS.params,
            filtered["params"]!!.jsonPrimitive.content,
        )
        val unfiltered = com.howdy.echowave.data.remote.innertube.searchBody("q", null)
        assertEquals(false, unfiltered.containsKey("params"))
    }

    @Test fun `artwork suffix upscales, unknown shapes pass`() {
        assertEquals(
            "https://x/abc=w540-h540",
            upgradeArtwork("https://x/abc=w60-h60"),
        )
        assertEquals("https://x/abc", upgradeArtwork("https://x/abc"))
        assertEquals(null, upgradeArtwork(null))
    }

    @Test fun `attachPoToken appends once`() {
        val base = com.howdy.echowave.domain.source.StreamInfo("v1", "http://a?x=1", poToken = "T")
        assertEquals("http://a?x=1&pot=T", attachPoToken(base).url)
        assertEquals("http://a?x=1&pot=T", attachPoToken(attachPoToken(base)).url)
        val noPot = com.howdy.echowave.domain.source.StreamInfo("v1", "http://a")
        assertEquals("http://a", attachPoToken(noPot).url)
    }

    @Test fun `two-row albums parse`() {        val root = json(
            """{"contents":{"sectionListRenderer":{"contents":[
              {"gridRenderer":{"items":[
                {"musicTwoRowItemRenderer":{
                  "title":{"runs":[{"text":"After Hours"}]},
                  "subtitle":{"runs":[{"text":"Album"},{"text":" • "},{"text":"The Weeknd"},{"text":" • "},{"text":"2020"}]},
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"ALB123"}},
                  "thumbnailRenderer":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://c=w60-h60"}]}}}
                }}
              ]}}
            ]}}}""",
        )
        val albums = parseTwoRowAlbums(root)
        assertEquals(1, albums.size)
        assertEquals("ALB123", albums[0].id)
        assertEquals("After Hours", albums[0].title)
        assertEquals("The Weeknd", albums[0].artist)
        assertEquals("http://c=w540-h540", albums[0].artworkUrl)
    }

    @Test fun `browse body carries id and visitor`() {
        val body = browseBody("FEmusic_charts", "P", "VIS")
        assertEquals("FEmusic_charts", body["browseId"]!!.jsonPrimitive.content)
        assertEquals("P", body["params"]!!.jsonPrimitive.content)
        val client = body["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("VIS", client["visitorData"]!!.jsonPrimitive.content)
        assertEquals("WEB_REMIX", client["clientName"]!!.jsonPrimitive.content)
    }

    @Test fun `video items flagged by kind and view counts`() {
        val root = json(
            """{"contents":{"sectionListRenderer":{"contents":[
              {"musicShelfRenderer":{"contents":[
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Song A"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Song"},{"text":" • "},{"text":"Artist"},{"text":" • "},{"text":"Album"}]}}}
                  ],
                  "playlistItemData":{"videoId":"song1"},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://a"}]}}}
                }},
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Video B"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Video"},{"text":" • "},{"text":"Artist"},{"text":" • "},{"text":"1.2M views"}]}}}
                  ],
                  "playlistItemData":{"videoId":"vid1"},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://b"}]}}}
                }}
              ]}}
            ]}}}""",
        )
        val results = parseSearchResponse(root)
        assertEquals(1, results.songs.size)
        assertEquals(1, results.videos.size)
        assertEquals(false, results.songs[0].isVideo)
        assertEquals("Album", results.songs[0].album)
        assertEquals(true, results.videos[0].isVideo)
    }

    @Test fun `sections keep every result present`() {
        val song = { id: String ->
            com.howdy.echowave.domain.model.SearchItem.Song(
                com.howdy.echowave.domain.model.Track(id, "t", "a"),
            )
        }
        val video = { id: String ->
            com.howdy.echowave.domain.model.SearchItem.Video(
                com.howdy.echowave.domain.model.Track(id, "t", "a", isVideo = true),
            )
        }
        val episode = { id: String ->
            com.howdy.echowave.domain.model.SearchItem.Episode(
                com.howdy.echowave.domain.model.Track(id, "t", "a", isEpisode = true),
            )
        }
        val ui = com.howdy.echowave.ui.search.SearchUiState(
            results = com.howdy.echowave.domain.model.SearchResults(
                topResult = song("top"),
                items = listOf(song("top"), song("s1"), video("v1"), episode("e1")),
            ),
        )
        assertEquals("top", (ui.topResult as com.howdy.echowave.domain.model.SearchItem.Song).track.id)
        assertEquals(listOf("top", "s1"), ui.songs.map { it.id })
        assertEquals(listOf("v1"), ui.videos.map { it.id })
        assertEquals(listOf("e1"), ui.episodes.map { it.id })
        assertEquals(true, ui.shows(com.howdy.echowave.ui.search.SearchSection.SONGS))
    }

    @Test fun `episode items flagged by kind or mention`() {
        val root = json(
            """{"contents":{"sectionListRenderer":{"contents":[
              {"musicShelfRenderer":{"contents":[
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Ep 12"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Episode"},{"text":" • "},{"text":"My Podcast"}]}}}
                  ],
                  "playlistItemData":{"videoId":"ep1"},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://e"}]}}}
                }}
              ]}}
            ]}}}""",
        )
        val results = parseSearchResponse(root)
        assertEquals(1, results.episodes.size)
        assertEquals(true, results.episodes[0].isEpisode)
        assertEquals(false, results.episodes[0].isVideo)
        assertEquals(null, results.episodes[0].album)
    }

    @Test fun `mood buttons parse with stripe and endpoint`() {
        val root = json(
            """{"contents":{"sectionListRenderer":{"contents":[
              {"gridRenderer":{
                "header":{"gridHeaderRenderer":{"title":{"runs":[{"text":"Moods"}]}}},
                "items":[
                  {"musicNavigationButtonRenderer":{
                    "buttonText":{"runs":[{"text":"Chill"}]},
                    "solid":{"leftStripeColor":4294967295},
                    "clickCommand":{"browseEndpoint":{"browseId":"GENRE1","params":"P1"}}
                  }},
                  {"musicNavigationButtonRenderer":{
                    "buttonText":{"runs":[{"text":"NoEndpoint"}]}
                  }}
                ]
              }}
            ]}}}""",
        )
        val moods = parseMoodGenres(root)
        assertEquals(1, moods.size)
        assertEquals("GENRE1", moods[0].id)
        assertEquals("Chill", moods[0].title)
        assertEquals(4294967295L, moods[0].color)
        assertEquals("P1", moods[0].params)
    }

    @Test fun `album artist playlist kinds parse with browse ids`() {
        val root = json(
            """{"contents":{"sectionListRenderer":{"contents":[
              {"musicShelfRenderer":{"contents":[
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"3 Idiots"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Album"},{"text":" • "},{"text":"Various Artists"}]}}}
                  ],
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"MPRE123"}},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://al"}]}}}
                }},
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Arijit Singh"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Artist"},{"text":" • "},{"text":"10M subscribers"}]}}}
                  ],
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"UC456"}},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://ar"}]}}}
                }},
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Bollywood Hits"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Playlist"},{"text":" • "},{"text":"YouTube Music"}]}}}
                  ],
                  "navigationEndpoint":{"browseEndpoint":{"browseId":"VLPL789"}},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://pl"}]}}}
                }},
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Ghost"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Album"}]}}}
                  ],
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://g"}]}}}
                }}
              ]}}
            ]}}}""",
        )
        val results = parseSearchResponse(root)
        // Id-less browse row is dropped; the rest surface as kinds.
        assertEquals(3, results.items.size)
        val album = results.items[0] as com.howdy.echowave.domain.model.SearchItem.Album
        assertEquals("MPRE123", album.id)
        assertEquals("3 Idiots", album.title)
        val artist = results.items[1] as com.howdy.echowave.domain.model.SearchItem.Artist
        assertEquals("UC456", artist.id)
        assertEquals("Arijit Singh", artist.name)
        val playlist = results.items[2] as com.howdy.echowave.domain.model.SearchItem.Playlist
        assertEquals("PL789", playlist.id)
    }

    @Test fun `display order is songs albums artists playlists videos episodes`() {
        val t = { id: String ->
            com.howdy.echowave.domain.model.Track(id, "t", "a")
        }
        val items = listOf(
            com.howdy.echowave.domain.model.SearchItem.Episode(t("e1")),
            com.howdy.echowave.domain.model.SearchItem.Video(t("v1")),
            com.howdy.echowave.domain.model.SearchItem.Playlist("p", "P", "A", null),
            com.howdy.echowave.domain.model.SearchItem.Artist("a", "A", null),
            com.howdy.echowave.domain.model.SearchItem.Album("al", "Al", "A", null),
            com.howdy.echowave.domain.model.SearchItem.Song(t("s1")),
            com.howdy.echowave.domain.model.SearchItem.Song(t("s2")),
            com.howdy.echowave.domain.model.SearchItem.Video(t("v2")),
        )
        val ordered = com.howdy.echowave.domain.model.orderSearchItems(items)
        assertEquals(
            listOf("s1", "s2", "al", "a", "p", "v1", "v2", "e1"),
            ordered.map {
                when (it) {
                    is com.howdy.echowave.domain.model.SearchItem.Song -> it.track.id
                    is com.howdy.echowave.domain.model.SearchItem.Video -> it.track.id
                    is com.howdy.echowave.domain.model.SearchItem.Episode -> it.track.id
                    is com.howdy.echowave.domain.model.SearchItem.Album -> it.id
                    is com.howdy.echowave.domain.model.SearchItem.Artist -> it.id
                    is com.howdy.echowave.domain.model.SearchItem.Playlist -> it.id
                }
            },
        )
    }

    @Test fun `top result prefers first song over earlier video`() {        val root = json(
            """{"contents":{"sectionListRenderer":{"contents":[
              {"musicShelfRenderer":{"contents":[
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Clip"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Video"},{"text":" • "},{"text":"Artist"}]}}}
                  ],
                  "playlistItemData":{"videoId":"vid9"},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://v"}]}}}
                }},
                {"musicResponsiveListItemRenderer":{
                  "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Song"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Song"},{"text":" • "},{"text":"Artist"}]}}}
                  ],
                  "playlistItemData":{"videoId":"song9"},
                  "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"http://s"}]}}}
                }}
              ]}}
            ]}}}""",
        )
        val results = parseSearchResponse(root)
        val top = results.topResult as com.howdy.echowave.domain.model.SearchItem.Song
        assertEquals("song9", top.track.id)
    }

    private fun runs(vararg texts: String) = buildJsonArray {
        texts.forEach { addJsonObject { put("text", it) } }
    }

    private fun flexCol(vararg texts: String) = buildJsonObject {
        put(
            "musicResponsiveListItemFlexColumnRenderer",
            buildJsonObject { put("text", buildJsonObject { put("runs", runs(*texts)) }) },
        )
    }

    private fun thumbs(url: String) = buildJsonObject {
        put(
            "musicThumbnailRenderer",
            buildJsonObject {
                put(
                    "thumbnail",
                    buildJsonObject {
                        put("thumbnails", buildJsonArray { addJsonObject { put("url", url) } })
                    },
                )
            },
        )
    }

    private fun songRowEl(id: String, title: String, artist: String = "Artist"): JsonObject =
        buildJsonObject {
            put(
                "musicResponsiveListItemRenderer",
                buildJsonObject {
                    put(
                        "flexColumns",
                        buildJsonArray {
                            add(flexCol(title))
                            add(flexCol("Song", artist))
                        },
                    )
                    put("playlistItemData", buildJsonObject { put("videoId", id) })
                    put("thumbnail", thumbs("http://a"))
                },
            )
        }

    private fun cardDoc(card: JsonObject, vararg rows: JsonObject): JsonObject = buildJsonObject {
        put(
            "contents",
            buildJsonObject {
                put(
                    "sectionListRenderer",
                    buildJsonObject {
                        put(
                            "contents",
                            buildJsonArray {
                                addJsonObject { put("musicCardShelfRenderer", card) }
                                if (rows.isNotEmpty()) {
                                    addJsonObject {
                                        put(
                                            "musicShelfRenderer",
                                            buildJsonObject { put("contents", JsonArray(rows.toList())) },
                                        )
                                    }
                                }
                            },
                        )
                    },
                )
            },
        )
    }

    private fun songCardEl(videoId: String, title: String, artist: String): JsonObject =
        buildJsonObject {
            put("title", buildJsonObject { put("runs", runs(title)) })
            put("subtitle", buildJsonObject { put("runs", runs("Song", "•", artist)) })
            put("thumbnail", thumbs("http://c"))
            put(
                "navigationEndpoint",
                buildJsonObject {
                    put("watchEndpoint", buildJsonObject { put("videoId", videoId) })
                },
            )
        }

    private fun albumCardEl(
        browseId: String,
        title: String,
        artist: String,
        params: String? = null,
    ): JsonObject =
        buildJsonObject {
            put("title", buildJsonObject { put("runs", runs(title)) })
            put("subtitle", buildJsonObject { put("runs", runs("Album", "•", artist)) })
            put("thumbnail", thumbs("http://c"))
            put(
                "onTap",
                buildJsonObject {
                    put("browseEndpoint", buildJsonObject {
                        put("browseId", browseId)
                        params?.let { put("params", it) }
                    })
                },
            )
        }

    @Test fun `per-kind caps keep lower shelves from starving`() {
        val rows = (1..25).map { songRowEl("s$it", "Song $it") } + songRowEl("late", "Late Song")
        val root = buildJsonObject {
            put(
                "contents",
                buildJsonObject {
                    put(
                        "sectionListRenderer",
                        buildJsonObject {
                            put(
                                "contents",
                                buildJsonArray {
                                    addJsonObject {
                                        put("musicShelfRenderer", buildJsonObject {
                                            put("contents", JsonArray(rows.take(25)))
                                        })
                                    }
                                    addJsonObject {
                                        put("musicShelfRenderer", buildJsonObject {
                                            put("contents", JsonArray(listOf(rows.last())))
                                        })
                                    }
                                },
                            )
                        },
                    )
                },
            )
        }
        // Legacy path preserves old first-N behavior exactly.
        assertEquals(25, parseSearchResponse(root, 25).items.size)
        // Capped path keeps songs from both shelves up to the songs budget.
        val capped = parseSearchResponse(root, 25, SearchCaps(songs = 20))
        assertEquals(20, capped.songs.size)
        assertEquals("s1", capped.songs.first().id)
        assertEquals("s20", capped.songs.last().id)
    }

    @Test fun `card shelf song becomes top result`() {
        val root = cardDoc(
            songCardEl("card1", "Haule Haule", "Sukhbir"),
            songRowEl("s1", "Other"),
        )
        val results = parseSearchResponse(root, 25, SEARCH_CAPS)
        val top = results.topResult as com.howdy.echowave.domain.model.SearchItem.Song
        assertEquals("card1", top.track.id)
        assertEquals("Sukhbir", top.track.artist)
        assertEquals(listOf("s1"), results.songs.map { it.id })
    }

    @Test fun `card shelf album resolves via browse id`() {
        val root = cardDoc(albumCardEl("MPRE999", "3 Idiots", "Various Artists", "ALBUM_PARAMS"))
        val results = parseSearchResponse(root)
        val top = results.topResult as com.howdy.echowave.domain.model.SearchItem.Album
        assertEquals("MPRE999", top.id)
        assertEquals("3 Idiots", top.title)
        assertEquals("ALBUM_PARAMS", top.browseParams)
    }

    @Test fun `unrecognized card falls back to first song`() {
        val card = buildJsonObject {
            put("title", buildJsonObject { put("runs", runs("???")) })
        }
        val root = cardDoc(card, songRowEl("s1", "Song One"))
        val results = parseSearchResponse(root)
        // Card without tappable endpoint is ignored; rows still parse.
        assertEquals(1, results.songs.size)
        assertEquals("s1", (results.topResult as com.howdy.echowave.domain.model.SearchItem.Song).track.id)
    }
}
