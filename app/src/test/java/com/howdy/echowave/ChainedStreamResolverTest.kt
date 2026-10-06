package com.howdy.echowave.domain.source

import com.howdy.echowave.core.common.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class OkResolver(private val url: String) : StreamResolver {
    override val name = "ok"
    override suspend fun resolve(trackId: String) = AppResult.Ok(StreamInfo(trackId, url))
}

private class FailResolver(private val msg: String) : StreamResolver {
    override val name = "fail"
    override suspend fun resolve(trackId: String): AppResult<StreamInfo> = AppResult.Err(msg)
}

class ChainedStreamResolverTest {
    @Test fun `falls through to first working link`() = runBlocking {
        val chain = ChainedStreamResolver(listOf(FailResolver("p dead"), OkResolver("https://a"), OkResolver("https://b")))
        val r = chain.resolve("v1")
        assertTrue(r is AppResult.Ok)
        assertEquals("https://a", (r as AppResult.Ok).value.url)
    }

    @Test fun `aggregates real failures skipping unwired stubs`() = runBlocking {
        val chain = ChainedStreamResolver(listOf(FailResolver("one"), FailResolver("two")))
        val r = chain.resolve("v1")
        assertTrue(r is AppResult.Err)
        assertEquals("[fail] one | [fail] two", (r as AppResult.Err).message)
    }

    @Test fun `all stubs reports port needed`() = runBlocking {
        val chain = ChainedStreamResolver(
            listOf(FailResolver("x not wired yet"), FailResolver("y not wired yet")),
        )
        val r = chain.resolve("v1")
        assertTrue(r is AppResult.Err)
    }

    @Test fun `empty chain reports misconfiguration`() = runBlocking {
        val r = ChainedStreamResolver(emptyList()).resolve("v1")
        assertTrue(r is AppResult.Err)
    }
}
