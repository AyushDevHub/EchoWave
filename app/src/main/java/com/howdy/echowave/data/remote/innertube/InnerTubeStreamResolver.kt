package com.howdy.echowave.data.remote.innertube

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.core.network.EchoWaveError
import com.howdy.echowave.core.network.userMessage
import com.howdy.echowave.domain.source.NoOpPoTokenProvider
import com.howdy.echowave.domain.source.PoTokenProvider
import com.howdy.echowave.domain.source.StreamInfo
import com.howdy.echowave.domain.source.StreamResolver
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject

/**
 * Primary resolver. Donor-measured client order (Echo-Music
 * STREAM_FALLBACK_CLIENTS head): VISIONOS -> ANDROID_VR -> TVHTML5,
 * then ANDROID/WEB with pot. First playable + probe-accepted URL wins.
 * Pot mints concurrently so token-free clients never wait for BotGuard.
 */
class InnerTubeStreamResolver(
    private val api: InnerTubeApi,
    private val poTokens: PoTokenProvider = NoOpPoTokenProvider(),
    private val visitors: VisitorStore = VisitorStore.inMemory(),
    private val probe: StreamProbe = StreamProbe(),
    private val registry: StreamRegistry = StreamRegistry(),
) : StreamResolver {
    override val name = "innertube-primary"

    override suspend fun resolve(trackId: String): AppResult<StreamInfo> = coroutineScope {
        val visitor = visitors.current()
        android.util.Log.d("EchoWaveResolve", "visitorPresent=${visitor != null}")
        val skipped = registry.excludedFor(trackId)
        if (skipped.isNotEmpty()) {
            android.util.Log.d("EchoWaveResolve", "skipped refused clients: $skipped")
        }
        // Mint concurrently: fast clients must not wait 8 s for BotGuard.
        val potJob = async {
            try {
                poTokens.getPoToken(trackId)
            } catch (_: Exception) {
                null
            }
        }
        val reasons = mutableListOf<String>()
        val tokenless: List<PlayerAttempt> = listOf(
            PlayerAttempt("visionos", VISIONOS_CLIENT_ID, VISIONOS_CLIENT_VERSION, playerBodyVisionOs(trackId, visitor), apiKey = null),
            PlayerAttempt("androidvr", ANDROID_VR_CLIENT_ID, ANDROID_VR_CLIENT_VERSION, playerBodyAndroidVr(trackId, visitor)),
            PlayerAttempt("tv", TV_CLIENT_ID, TV_CLIENT_VERSION, playerBodyTv(trackId, visitor)),
        ).filter { it.label !in skipped }
        for (attempt in tokenless) {
            val parsed = playerAttempt(trackId, attempt, skipPot = true)
            val info = parsed.getOrNull()
            if (info != null && probeAccept(trackId, attempt.label, info)) {
                registry.record(info.url, trackId, attempt.label)
                return@coroutineScope AppResult.Ok(info)
            }
            parsed.exceptionOrNull()?.let { reasons += "${attempt.label}:${it.message}" }
        }
        val token = potJob.await()
        if (token != null) {
            android.util.Log.d("EchoWaveResolve", "poToken available for trackId=$trackId")
        }
        val potted: List<PlayerAttempt> = listOf(
            PlayerAttempt("android", ANDROID_CLIENT_ID, ANDROID_CLIENT_VERSION, playerBodyAndroid(trackId, token?.playerRequestPoToken, visitor)),
            PlayerAttempt("web", INNERTUBE_CLIENT_ID, INNERTUBE_CLIENT_VERSION, playerBody(trackId, token?.playerRequestPoToken, visitor)),
        ).filter { it.label !in registry.excludedFor(trackId) }
        for (attempt in potted) {
            val parsed = playerAttempt(trackId, attempt, skipPot = false)
            val info = parsed.getOrNull()
            if (info != null && probeAccept(trackId, attempt.label, info)) {
                val withPot = attachPoToken(
                    info.copy(poToken = token?.streamingDataPoToken ?: info.poToken),
                )
                logResolved(trackId, attempt.label, withPot)
                registry.record(withPot.url, trackId, attempt.label)
                return@coroutineScope AppResult.Ok(withPot)
            }
            parsed.exceptionOrNull()?.let { reasons += "${attempt.label}:${it.message}" }
        }
        AppResult.Err(
            EchoWaveError.StreamUnavailable(trackId).userMessage() + " (${reasons.joinToString(" | ")})",
        )
    }

    /** Player call + parse. Returns success, or failure with a short reason. */
    private data class PlayerAttempt(
        val label: String,
        val clientId: String,
        val clientVersion: String,
        val body: JsonObject,
        /** Null omits ?key=. Donor sends no key on main paths; VISIONOS 400s with it. */
        val apiKey: String? = INNERTUBE_API_KEY,
    )

    private suspend fun playerAttempt(
        trackId: String,
        attempt: PlayerAttempt,
        skipPot: Boolean,
    ): Result<StreamInfo> {
        return try {
            val root = api.player(
                key = attempt.apiKey,
                clientId = attempt.clientId,
                clientVersion = attempt.clientVersion,
                body = attempt.body,
            )
            visitors.offer(extractVisitorData(root))
            when (val p = parsePlayerResponse(trackId, root)) {
                is PlayerParse.Playable -> {
                    logResolved(trackId, attempt.label, p.info)
                    Result.success(p.info)
                }
                is PlayerParse.Unplayable -> {
                    android.util.Log.e("EchoWaveResolve", "innerTube ${attempt.label} unplayable trackId=$trackId reason=${p.reason}")
                    Result.failure(Exception(p.reason))
                }
            }
        } catch (_: Exception) {
            android.util.Log.e("EchoWaveResolve", "innerTube ${attempt.label} network fail")
            Result.failure(Exception(EchoWaveError.Network("resolve failed").userMessage()))
        }
    }

    private suspend fun probeAccept(trackId: String, label: String, info: StreamInfo): Boolean {
        return when (val v = probe.probe(info.url)) {
            is StreamProbe.Verdict.Accept -> {
                android.util.Log.d("EchoWaveResolve", "probe accept trackId=$trackId via=$label")
                true
            }
            is StreamProbe.Verdict.Reject -> {
                android.util.Log.e("EchoWaveResolve", "probe reject trackId=$trackId via=$label code=${v.code}")
                false
            }
        }
    }

    private fun logResolved(trackId: String, label: String, info: StreamInfo) {
        android.util.Log.d(
            "EchoWaveResolve",
            "innerTube resolve OK trackId=$trackId via=$label " +
                "poTokenPresent=${info.poToken != null} " +
                "potAttached=${info.url.contains("pot=")}",
        )
        try {
            val uri = android.net.Uri.parse(info.url)
            android.util.Log.d(
                "EchoWaveResolve",
                "resolve OK trackId=$trackId via=$label mime=${info.mimeType} " +
                    "host=${uri.host} hasSig=${uri.getQueryParameter("sig") != null} " +
                    "hasN=${uri.getQueryParameter("n") != null} " +
                    "params=${uri.queryParameterNames.sorted()}",
            )
        } catch (_: Exception) {
        }
    }
}

/**
 * Appends the Proof-of-Origin token as `pot`. Pure + unit-tested.
 * Token value is never logged anywhere.
 */
fun attachPoToken(info: StreamInfo): StreamInfo {
    val pot = info.poToken
    if (pot.isNullOrEmpty() || info.url.contains("pot=")) return info
    val sep = if (info.url.contains("?")) "&" else "?"
    return info.copy(url = info.url + sep + "pot=" + pot)
}
