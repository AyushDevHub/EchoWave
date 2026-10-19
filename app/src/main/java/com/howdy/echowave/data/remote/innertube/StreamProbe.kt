package com.howdy.echowave.data.remote.innertube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Pre-flight URL check mirroring the media request (donor: validateStatus).
 * GET with a byte range, dressed with the minting client's headers. Some
 * media hosts reject HEAD even when the same ranged GET is playable. Never throws.
 */
class StreamProbe(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build(),
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    sealed interface Verdict {
        data object Accept : Verdict
        data class Reject(val code: Int) : Verdict
    }

    suspend fun probe(url: String): Verdict = withContext(ioDispatcher) {
        val headers = PlayerClient.forStreamUrl(url).mediaHeaders()
        // Donor rule: a preview-only URL serves its first chunk fine and 403s
        // past ~1 MiB, so probe the LAST byte when clen is known. A URL that
        // serves its final byte is not a truncated preview.
        val total = runCatching {
            url.toHttpUrlOrNull()?.queryParameter("clen")?.toLongOrNull()?.takeIf { it > 0 }
        }.getOrNull()
        val range = if (total != null && total > 0) {
            // HTTP byte-range endpoints are inclusive. Keep the probe inside
            // the resource; asking for `total` is one byte beyond its end.
            "bytes=${total - 1}-${total - 1}"
        } else {
            "bytes=0-${PROBE_BYTES - 1}"
        }
        android.util.Log.d("EchoWaveProbe", "probe range=$range clenKnown=${total != null}")
        try {
            val req = Request.Builder().get().url(url)
                .header("Range", range)
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()
            http.newCall(req).execute().use { resp ->
                val code = resp.code
                if (acceptHttpCode(code)) Verdict.Accept else Verdict.Reject(code)
            }
        } catch (_: IOException) {
            // Timeout/reset: ExoPlayer has its own retry; don't burn a client.
            Verdict.Accept
        } catch (_: Exception) {
            Verdict.Reject(-1)
        }
    }

    companion object {
        const val PROBE_BYTES = 512L * 1024

        /** 2xx + 405 (HEAD refused outright; GET will follow) accept. */
        fun acceptHttpCode(code: Int): Boolean = (code in 200..299) || code == 405
    }
}
