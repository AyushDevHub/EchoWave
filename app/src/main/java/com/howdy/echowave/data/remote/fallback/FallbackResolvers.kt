package com.howdy.echowave.data.remote.fallback

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.source.StreamInfo
import com.howdy.echowave.domain.source.StreamResolver

/**
 * Backup links in the resolve chain. Stubs until donor logic is ported
 * (NewPipe-Extractor patterns / BravePipe-style backup engine, see CREDITS.md).
 * The chain tries them in order after the InnerTube primary.
 */
class NewPipeFallbackResolver : StreamResolver {
    override val name = "newpipe-fallback"
    override suspend fun resolve(trackId: String): AppResult<StreamInfo> =
        AppResult.Err("NewPipe fallback not wired yet")
}

class BravePipeFallbackResolver : StreamResolver {
    override val name = "bravepipe-fallback"
    override suspend fun resolve(trackId: String): AppResult<StreamInfo> =
        AppResult.Err("BravePipe fallback not wired yet")
}
