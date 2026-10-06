package com.howdy.echowave.data.remote.potoken

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class JsCodecTest {
    @Test fun `u8 round trip`() {
        assertEquals("new Uint8Array([65])", JsCodec.stringToU8("A"))
        assertEquals("YWJj", JsCodec.u8ToBase64("97,98,99"))
    }

    @Test fun `integrity token parses to u8 plus seconds`() {
        // "hi" -> aGk= in standard base64
        val (u8, seconds) = JsCodec.parseIntegrityTokenData("""["aGk=",3600]""")
        assertEquals("new Uint8Array([104,105])", u8)
        assertEquals(3600L, seconds)
    }

    @Test fun `player bodies carry poToken only when known`() {
        val with = com.howdy.echowave.data.remote.innertube.playerBodyAndroid("v", "POT")
        assertEquals(
            "POT",
            with["serviceIntegrityDimensions"]!!.jsonObject["poToken"]!!.jsonPrimitive.content,
        )
        val without = com.howdy.echowave.data.remote.innertube.playerBodyAndroid("v", null)
        assertEquals(false, without.containsKey("serviceIntegrityDimensions"))
    }
}
