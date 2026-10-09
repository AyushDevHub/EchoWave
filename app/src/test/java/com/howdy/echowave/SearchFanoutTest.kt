package com.howdy.echowave.data.remote.innertube

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.SearchFilter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun shelfRowEl(id: String, title: String): JsonObject = buildJsonObject {
    put(
        "musicResponsiveListItemRenderer",
        buildJsonObject {
            put(
                "flexColumns",
                buildJsonArray {
                    addJsonObject {
                        put(
                            "musicResponsiveListItemFlexColumnRenderer",
                            buildJsonObject {
                                put(
                                    "text",
                                    buildJsonObject {
                                        put(
                                            "runs",
                                            buildJsonArray {
                                                addJsonObject { put("text", title) }
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }
                    addJsonObject {
                        put(
                            "musicResponsiveListItemFlexColumnRenderer",
                            buildJsonObject {
                                put(
                                    "text",
                                    buildJsonObject {
                                        put(
                                            "runs",
                                            buildJsonArray {
                                                addJsonObject { put("text", "Song") }
                                                addJsonObject { put("text", "Artist") }
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }
                },
            )
            put("playlistItemData", buildJsonObject { put("videoId", id) })
            put(
                "thumbnail",
                buildJsonObject {
                    put(
                        "musicThumbnailRenderer",
                        buildJsonObject {
                            put(
                                "thumbnail",
                                buildJsonObject {
                                    put(
                                        "thumbnails",
                                        buildJsonArray {
                                            addJsonObject { put("url", "http://a") }
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            )
        },
    )
}

private fun shelfDocEl(vararg rows: JsonObject): JsonObject = buildJsonObject {
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
                                put(
                                    "musicShelfRenderer",
                                    buildJsonObject {
                                        put(
                                            "contents",
                                            buildJsonArray { rows.forEach { add(it) } },
                                        )
                                    },
                                )
                            }
                        },
                    )
                },
            )
        },
    )
}

private class FakeApi(
    private val handler: (params: String?) -> JsonObject,
) : InnerTubeApi {
    var calls = 0
    val queries = mutableListOf<String>()
    override suspend fun search(
        key: String?,
        prettyPrint: Boolean,
        clientId: String?,
        clientVersion: String?,
        body: JsonObject,
    ): JsonObject {
        calls++
        val params = (body["params"] as? JsonPrimitive)?.content
        (body["query"] as? JsonPrimitive)?.content?.let(queries::add)
        return handler(params)
    }

    override suspend fun player(
        key: String?,
        prettyPrint: Boolean,
        clientId: String?,
        clientVersion: String?,
        body: JsonObject,
    ): JsonObject = JsonObject(emptyMap())

    override suspend fun browse(
        key: String?,
        prettyPrint: Boolean,
        clientId: String?,
        clientVersion: String?,
        body: JsonObject,
    ): JsonObject = JsonObject(emptyMap())

    override suspend fun next(
        key: String?,
        prettyPrint: Boolean,
        clientId: String?,
        clientVersion: String?,
        body: JsonObject,
    ): JsonObject = JsonObject(emptyMap())
}

class SearchFanoutTest {
    @Before fun silenceLogs() {
        searchLog = {}
    }

    @After fun restoreLogs() {
        searchLog = { android.util.Log.d("EchoWaveSearch", it) }
    }

    private fun source(handler: (String?) -> JsonObject) =
        InnerTubeMusicSource(FakeApi(handler), VisitorStore.inMemory())

    private fun doc(vararg rows: JsonObject) = shelfDocEl(*rows)

    @Test fun `fan-out merges legs and dedupes`() = runBlocking {
        val src = source { params ->
            when (params) {
                SearchFilter.SONGS.params -> doc(shelfRowEl("s1", "Shared"), shelfRowEl("s2", "Extra"))
                SearchFilter.ALBUMS.params -> doc(shelfRowEl("s3", "Album Song"))
                else -> doc(shelfRowEl("s1", "Shared"))
            }
        }
        val r = src.search("q", 25)
        assertTrue(r is AppResult.Ok)
        val ids = (r as AppResult.Ok).value.tracks.map { it.id }
        assertEquals(listOf("s1", "s2", "s3"), ids)
    }

    @Test fun `movie search adds soundtrack and songs focused queries`() = runBlocking {
        val api = FakeApi { doc() }
        val src = InnerTubeMusicSource(api, VisitorStore.inMemory())
        val r = src.search("Dhadkan", 25)
        assertTrue(r is AppResult.Ok)
        assertTrue(api.queries.contains("Dhadkan soundtrack"))
        assertTrue(api.queries.contains("Dhadkan Hindi movie songs"))
        assertTrue(api.queries.contains("Dhadkan songs"))
        assertTrue(api.queries.contains("Dhadkan original motion picture soundtrack"))
    }

    @Test fun `merge dedupes promotional variants by track identity but keeps live version`() = runBlocking {
        val src = source {
            doc(
                shelfRowEl("official-audio", "Haule Haule"),
                shelfRowEl("official-video", "Haule Haule Official Video"),
                shelfRowEl("live", "Haule Haule (Live)"),
            )
        }
        val result = src.search("Haule", 25) as AppResult.Ok
        assertEquals(listOf("official-audio", "live"), result.value.songs.map { it.id })
    }

    @Test fun `dead filtered leg keeps base results`() = runBlocking {
        val src = source { params ->
            if (params == SearchFilter.SONGS.params) throw RuntimeException("boom")
            doc(shelfRowEl("s1", "Base"))
        }
        val r = src.search("q", 25)
        assertTrue(r is AppResult.Ok)
        assertEquals(listOf("s1"), (r as AppResult.Ok).value.tracks.map { it.id })
    }

    @Test fun `dead base leg fails the search`() = runBlocking {
        val src = source { params ->
            if (params == null) throw RuntimeException("down")
            doc(shelfRowEl("s9", "Filtered Only"))
        }
        val r = src.search("q", 25)
        // Base carries ranking + top result; without it there is nothing to show.
        assertTrue(r is AppResult.Err)
    }

    @Test fun `filtered search hits params body`() = runBlocking {
        var seenParams: String? = "unset"
        val src = source { params ->
            seenParams = params
            doc(shelfRowEl("s1", "Song"))
        }
        val r = src.searchFiltered("q", SearchFilter.ALBUMS, 10)
        assertTrue(r is AppResult.Ok)
        assertEquals(SearchFilter.ALBUMS.params, seenParams)
    }
}
