package com.howdy.echowave.domain.source

import com.howdy.echowave.core.common.AppResult

data class StreamInfo(
    val trackId: String,
    val url: String,
    val mimeType: String? = null,
    val expiresAtMs: Long? = null,
    /** Proof-of-Origin token from serviceIntegrityDimensions, appended as &pot=. Never logged. */
    val poToken: String? = null,
)

/**
 * Resolves a Track ID into something ExoPlayer can play.
 * Chain: primary InnerTube -> NewPipe-style -> BravePipe-style backup.
 * Each link is replaceable; a source break must not rewrite the app.
 */
interface StreamResolver {
    val name: String
    suspend fun resolve(trackId: String): AppResult<StreamInfo>
}

class ChainedStreamResolver(
    private val chain: List<StreamResolver>,
) : StreamResolver {
    override val name = "chained(${chain.joinToString("+") { it.name }})"

    override suspend fun resolve(trackId: String): AppResult<StreamInfo> {
        if (chain.isEmpty()) return AppResult.Err("no stream resolvers configured")
        val failures = mutableListOf<String>()
        for (link in chain) {
            when (val r = runCatching { link.resolve(trackId) }.getOrElse {
                AppResult.Err("resolver ${link.name} crashed", it)
            }) {
                is AppResult.Ok -> return r
                // Skip unwired stubs in the message — they are placeholders, not diagnoses.
                is AppResult.Err -> if (!r.message.contains("not wired yet")) failures += "[${link.name}] ${r.message}"
            }
        }
        if (failures.isEmpty()) return AppResult.Err("all resolvers unwired — port InnerTube stream resolver")
        return AppResult.Err(failures.joinToString(" | "))
    }
}
