package com.howdy.echowave.data.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener

/**
 * Bounded-range media fetch (donor: ChunkedDataSource). googlevideo paces
 * unbounded reads down to playback speed and refuses them outright on some
 * tracks; bounded ranges are served at line rate. Total size comes from the
 * `clen` URL param; anything else passes through untouched.
 *
 * @param onRefused called with the URL when the server refuses a range, so
 *   the resolver can exclude that minting client for the track.
 */
@UnstableApi
class ChunkedDataSource(
    private val upstream: DataSource,
    private val chunkBytes: Long,
    private val onRefused: (String) -> Unit = {},
) : DataSource {
    private var baseSpec: DataSpec? = null
    private var position = 0L
    private var bytesRemaining = 0L
    private var chunkRemaining = 0L
    private var chunkOpen = false
    private var passthrough = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        baseSpec = dataSpec
        position = dataSpec.position
        val total = dataSpec.uri.getQueryParameter("clen")?.toLongOrNull()
        if (total == null) {
            passthrough = true
            chunkOpen = true
            return try {
                upstream.open(dataSpec)
            } catch (e: Exception) {
                report(dataSpec, e)
                throw e
            }
        }
        passthrough = false
        val end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) total
        else minOf(total, position + dataSpec.length)
        bytesRemaining = (end - position).coerceAtLeast(0L)
        if (bytesRemaining > 0) openChunk()
        return bytesRemaining
    }

    private fun openChunk() {
        val length = minOf(chunkBytes, bytesRemaining)
        val spec = requireNotNull(baseSpec).buildUpon().setPosition(position).setLength(length).build()
        try {
            upstream.open(spec)
        } catch (e: Exception) {
            report(spec, e)
            throw e
        }
        chunkRemaining = length
        chunkOpen = true
    }

    private fun report(spec: DataSpec, e: Exception) {
        if (e is HttpDataSource.InvalidResponseCodeException) {
            val url = spec.uri.toString()
            android.util.Log.w(TAG, "range refused status=${e.responseCode} host=${spec.uri.host}")
            runCatching { onRefused(url) }
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (passthrough) return upstream.read(buffer, offset, length)
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        repeat(MAX_EMPTY_RANGES) {
            if (chunkRemaining == 0L) {
                closeChunk()
                openChunk()
            }
            val read = upstream.read(buffer, offset, minOf(length.toLong(), chunkRemaining).toInt())
            if (read != C.RESULT_END_OF_INPUT) {
                position += read
                chunkRemaining -= read
                bytesRemaining -= read
                return read
            }
            chunkRemaining = 0L
        }
        return C.RESULT_END_OF_INPUT
    }

    private fun closeChunk() {
        if (chunkOpen) {
            upstream.close()
            chunkOpen = false
        }
    }

    override fun getUri(): Uri? = upstream.uri ?: baseSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        closeChunk()
        baseSpec = null
        bytesRemaining = 0L
        chunkRemaining = 0L
    }

    class Factory(
        private val upstream: DataSource.Factory,
        private val chunkBytes: Long = 512L * 1024,
        private val onRefused: (String) -> Unit = {},
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            ChunkedDataSource(upstream.createDataSource(), chunkBytes, onRefused)
    }

    companion object {
        private const val TAG = "EchoWaveChunk"
        private const val MAX_EMPTY_RANGES = 3
    }
}
