package com.howdy.echowave.data.remote.innertube

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val tracks = parseSearchResponse(root)
        assertEquals(1, tracks.size)
        assertEquals("abc123", tracks[0].id)
        assertEquals("Blinding Lights", tracks[0].title)
        assertEquals("The Weeknd", tracks[0].artist)
        assertEquals("After Hours", tracks[0].album)
        assertEquals("http://big", tracks[0].artworkUrl)
        assertEquals(200_000L, tracks[0].durationMs)
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

    @Test fun `search body carries visitorData when known`() {
        val with = com.howdy.echowave.data.remote.innertube.searchBody("q", "VIS")
        val client = with["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("VIS", client["visitorData"]!!.jsonPrimitive.content)
        val without = com.howdy.echowave.data.remote.innertube.searchBody("q", null)
        assertEquals(
            false,
            without["context"]!!.jsonObject["client"]!!.jsonObject.containsKey("visitorData"),
        )
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
        val tracks = parseSearchResponse(root)
        assertEquals(2, tracks.size)
        assertEquals(false, tracks[0].isVideo)
        assertEquals("Album", tracks[0].album)
        assertEquals(true, tracks[1].isVideo)
    }

    @Test fun `sections keep every result present`() {
        val t = { id: String, video: Boolean, episode: Boolean ->
            com.howdy.echowave.domain.model.Track(id, "t", "a", isVideo = video, isEpisode = episode)
        }
        val ui = com.howdy.echowave.ui.search.SearchUiState(
            results = listOf(
                t("top", false, false),
                t("s1", false, false),
                t("v1", true, false),
                t("e1", false, true),
            ),
        )
        assertEquals("top", ui.topResult?.id)
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
        val tracks = parseSearchResponse(root)
        assertEquals(1, tracks.size)
        assertEquals(true, tracks[0].isEpisode)
        assertEquals(false, tracks[0].isVideo)
        assertEquals(null, tracks[0].album)
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
}
