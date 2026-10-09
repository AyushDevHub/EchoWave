package com.howdy.echowave

import com.howdy.echowave.data.remote.innertube.InnerTubeNextParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class InnerTubeNextParserTest {
    @Test
    fun parsesPlaylistPanelVideoRenderersFromNextResponse() {
        val sampleJson = """
        {
            "contents": {
                "singleColumnMusicWatchNextResultsRenderer": {
                    "tabbedRenderer": {
                        "watchNextTabbedResultsRenderer": {
                            "tabs": [
                                {
                                    "tabRenderer": {
                                        "content": {
                                            "musicQueueRenderer": {
                                                "content": {
                                                    "playlistPanelRenderer": {
                                                        "contents": [
                                                            {
                                                                "playlistPanelVideoRenderer": {
                                                                    "videoId": "fevicol123",
                                                                    "title": { "runs": [ { "text": "Fevicol Se" } ] },
                                                                    "shortBylineText": { "runs": [ { "text": "Mamta Sharma" }, { "text": " • " }, { "text": "Dabangg 2" } ] },
                                                                    "lengthText": { "runs": [ { "text": "4:45" } ] },
                                                                    "thumbnail": { "thumbnails": [ { "url": "https://img/thumb.jpg=w120-h120" } ] }
                                                                }
                                                            },
                                                            {
                                                                "playlistPanelVideoRenderer": {
                                                                    "videoId": "kajra456",
                                                                    "title": { "runs": [ { "text": "Kajra Re" } ] },
                                                                    "shortBylineText": { "runs": [ { "text": "Alisha Chinai" } ] },
                                                                    "lengthText": { "runs": [ { "text": "5:12" } ] },
                                                                    "thumbnail": { "thumbnails": [ { "url": "https://img/kajra.jpg" } ] }
                                                                }
                                                            }
                                                        ]
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            ]
                        }
                    }
                }
            }
        }
        """.trimIndent()

        val root = Json.parseToJsonElement(sampleJson).jsonObject
        val tracks = InnerTubeNextParser.parse(root)

        assertEquals(2, tracks.size)
        val first = tracks[0]
        assertEquals("fevicol123", first.id)
        assertEquals("Fevicol Se", first.title)
        assertEquals("Mamta Sharma", first.artist)
        assertEquals("Dabangg 2", first.album)
        assertEquals(285_000L, first.durationMs)
        assertEquals("https://img/thumb.jpg=w540-h540", first.artworkUrl)

        val second = tracks[1]
        assertEquals("kajra456", second.id)
        assertEquals("Kajra Re", second.title)
        assertEquals("Alisha Chinai", second.artist)
        assertEquals(312_000L, second.durationMs)
    }
}
