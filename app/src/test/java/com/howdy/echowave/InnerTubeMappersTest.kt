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

    @Test fun `attachPoToken appends once`() {
        val base = com.howdy.echowave.domain.source.StreamInfo("v1", "http://a?x=1", poToken = "T")
        assertEquals("http://a?x=1&pot=T", attachPoToken(base).url)
        assertEquals("http://a?x=1&pot=T", attachPoToken(attachPoToken(base)).url)
        val noPot = com.howdy.echowave.domain.source.StreamInfo("v1", "http://a")
        assertEquals("http://a", attachPoToken(noPot).url)
    }
}
