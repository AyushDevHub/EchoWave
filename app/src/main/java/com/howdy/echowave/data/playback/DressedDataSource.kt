package com.howdy.echowave.data.playback

import android.net.Uri
import android.util.Log
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.common.util.UnstableApi
import com.howdy.echowave.data.remote.innertube.PlayerClient

/**
 * Diagnostic + dressing wrapper around the HTTP fetch.
 * Per open(): dresses headers from PlayerClient.forStreamUrl (donor rule:
 * the fetch must wear the minting client's identity) and logs ONLY safe
 * metadata — never URL values, tokens, or cookies.
 */
@UnstableApi
class DressedDataSource(
    private val delegate: DefaultHttpDataSource,
) : DataSource {
    override fun addTransferListener(transferListener: TransferListener) {
        delegate.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val uri = dataSpec.uri
        val url = uri.toString()
        val client = PlayerClient.forStreamUrl(url)
        // Per-request dressing on the spec itself: the shared connection
        // keeps no state, so concurrent loads can't cross-contaminate.
        val dressed = dataSpec.buildUpon()
            .setHttpRequestHeaders(client.mediaHeaders())
            .build()
        val range = when {
            dataSpec.position > 0 && dataSpec.length > 0 ->
                "bytes=${dataSpec.position}-${dataSpec.position + dataSpec.length - 1}"
            dataSpec.position > 0 -> "bytes=${dataSpec.position}-"
            dataSpec.length > 0 -> "bytes=0-${dataSpec.length - 1}"
            else -> "none(unbounded)"
        }
        // All dressed values are our own public constants + range math — safe to log whole.
        Log.d(TAG, "headers ua=${client.userAgent} origin=${client.origin} referer=${client.referer} range=$range")
        val c = uri.getQueryParameter("c")
        val cver = uri.getQueryParameter("cver")
        Log.d(
            TAG,
            "open host=${uri.host} path=${uri.path} method=${dataSpec.httpMethod} " +
                "dress=${client.clientName}/${client.clientVersion} " +
                "origin=${client.origin != null} referer=${client.referer != null} " +
                "range=$range pot=${uri.getQueryParameter("pot") != null} " +
                "sig=${uri.getQueryParameter("sig") != null} c=$c cver=$cver",
        )
        try {
            val length = delegate.open(dressed)
            val finalUri = delegate.uri
            Log.d(
                TAG,
                "opened length=$length redirected=${finalUri != null && finalUri.host != uri.host} " +
                    "finalHost=${finalUri?.host}",
            )
            return length
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            val finalUri = runCatching { delegate.uri }.getOrNull()
            val respHeaders = runCatching { delegate.responseHeaders }.getOrDefault(emptyMap())
            Log.e(
                TAG,
                "refused status=${e.responseCode} host=${uri.host} " +
                    "dress=${client.clientName}/${client.clientVersion} " +
                    "finalHost=${finalUri?.host}",
            )
            // Response diagnostics: header NAMES always; VALUES only for the
            // non-sensitive allowlist. Never cookies / auth / tokens.
            Log.e(TAG, "respHeaders keys=${respHeaders.keys.sorted()}")
            for (key in listOf("Content-Type", "Server", "X-Goog-Correlation-Id", "X-YouTube-Error-Code")) {
                respHeaders.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.let {
                    Log.e(TAG, "respHeader $key=${it.value.firstOrNull()}")
                }
            }
            throw e
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        delegate.read(buffer, offset, length)

    override fun getUri(): Uri? = delegate.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate.responseHeaders

    override fun close() = delegate.close()

    class Factory : DataSource.Factory {
        override fun createDataSource(): DataSource {
            val http = DefaultHttpDataSource.Factory()
                .setUserAgent(PlayerClient.ANDROID.userAgent)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(20_000)
                .createDataSource()
            return DressedDataSource(http)
        }
    }

    companion object {
        private const val TAG = "EchoWaveFetch"
    }
}
