package com.howdy.echowave.data.remote.fake

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.source.MusicSource
import com.howdy.echowave.domain.source.StreamInfo
import com.howdy.echowave.domain.source.StreamResolver

/** Unblocks UI + playback wiring before the InnerTube port lands. */
class FakeMusicSource : MusicSource {
    override val name = "fake"
    private val demo = listOf(
        Track("demo-1", "Chamak Challo", "Artist 1", "Ra.One", null, 210_000),
        Track("demo-2", "Desi Girl", "Artist 2", "Dostana", null, 180_000),
    )
    override suspend fun search(query: String, limit: Int) =
        AppResult.Ok(demo.filter { it.title.contains(query, true) || it.artist.contains(query, true) })
    override suspend fun getTrack(id: String) =
        demo.find { it.id == id }?.let { AppResult.Ok(it) } ?: AppResult.Err("not found")
}

class FakeStreamResolver : StreamResolver {
    override val name = "fake"
    override suspend fun resolve(trackId: String): AppResult<StreamInfo> =
        AppResult.Err("fake has no audio URL — land InnerTube resolver for real sound")
}
