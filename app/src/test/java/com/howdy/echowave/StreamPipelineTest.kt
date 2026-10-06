package com.howdy.echowave.data.remote.innertube

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamPipelineTest {
    @Test fun `registry excludes refused client per track with expiry`() {
        var now = 0L
        val reg = StreamRegistry(clockMs = { now })
        reg.record("http://u1", "v1", "visionos")
        assertEquals("v1", reg.onRefused("http://u1"))
        assertEquals(setOf("visionos"), reg.excludedFor("v1"))
        assertEquals(emptySet<String>(), reg.excludedFor("v2"))
        now = 10 * 60 * 1000L + 1
        assertEquals(emptySet<String>(), reg.excludedFor("v1"))
    }

    @Test fun `registry ignores unknown urls`() {
        val reg = StreamRegistry()
        assertEquals(null, reg.onRefused("http://unknown"))
    }

    @Test fun `probe accept rules`() {
        assertTrue(StreamProbe.acceptHttpCode(200))
        assertTrue(StreamProbe.acceptHttpCode(206))
        assertTrue(StreamProbe.acceptHttpCode(405))
        assertEquals(false, StreamProbe.acceptHttpCode(403))
        assertEquals(false, StreamProbe.acceptHttpCode(410))
        assertEquals(false, StreamProbe.acceptHttpCode(500))
    }

    @Test fun `donor client bodies carry pinned identities`() {
        val vr = playerBodyAndroidVr("v", "VIS")
        val vrClient = vr["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("ANDROID_VR", vrClient["clientName"]!!.jsonPrimitive.content)
        assertEquals("1.65.10", vrClient["clientVersion"]!!.jsonPrimitive.content)
        assertEquals("VIS", vrClient["visitorData"]!!.jsonPrimitive.content)

        val tv = playerBodyTv("v", null)
        val tvClient = tv["context"]!!.jsonObject["client"]!!.jsonObject
        assertEquals("TVHTML5", tvClient["clientName"]!!.jsonPrimitive.content)
        assertEquals(false, tvClient.containsKey("visitorData"))

        val vis = playerBodyVisionOs("v", null)
        assertEquals(
            "VISIONOS",
            vis["context"]!!.jsonObject["client"]!!.jsonObject["clientName"]!!.jsonPrimitive.content,
        )
    }
}
