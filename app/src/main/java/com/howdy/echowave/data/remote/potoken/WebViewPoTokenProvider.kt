package com.howdy.echowave.data.remote.potoken

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minting half of the donor flow. The WebView impl runs BotGuard once per
 * session; tests inject fakes.
 */
interface BotGuardMinter {
    /** Mint the session-bound streaming pot; returns pot + expiry seconds. */
    suspend fun mintStreamingPot(sessionId: String): Pair<String, Long>

    /** Mint the video-bound player pot. */
    suspend fun mintPlayerPot(videoId: String): String
    fun close()
}

/**
 * Local WebView BotGuard PO-token minting. Port of Echo-Music's
 * utils/potoken/{PoTokenGenerator,PoTokenWebView}.kt (GPL-3.0, see CREDITS.md).
 *
 * Contract: never throws — every failure (no WebView, timeout, JS error)
 * resolves to null so resolve falls through to the next chain link.
 */
class WebViewPoTokenProvider(
    appContext: Context?,
    private val timeoutMs: Long = 8000L,
    private val cache: PoTokenCache = PoTokenCache(),
    private val openMinter: suspend () -> BotGuardMinter = { BotGuardWebView.open(appContext!!) },
    private val sessionOf: () -> String = { ANONYMOUS_SESSION },
) : com.howdy.echowave.domain.source.PoTokenProvider {
    private val mutex = Mutex()
    private var minter: BotGuardMinter? = null
    private var lastSession: String? = null
    // Default dispatcher on purpose: the provider itself never needs Main
    // (WebView impls switch internally). Keeps JVM unit tests green.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun prewarm() {
        scope.launch { runCatching { getPoToken(PREWARM_ID) } }
    }

    override suspend fun getPoToken(videoId: String): com.howdy.echowave.domain.source.PoToken? {
        return try {
            withTimeout(timeoutMs) {
                mutex.withLock {
                    val session = sessionOf()
                    if (lastSession != null && lastSession != session) {
                        // New visitor/session: old session-bound pot no longer valid.
                        cache.clear()
                    }
                    lastSession = session
                    val m = try {
                        minter ?: openMinter().also { minter = it }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "poToken minter open failed: ${e.javaClass.simpleName}")
                        return@withTimeout null
                    }
                    // Streaming pot is session-bound and cached; the player pot
                    // is video-bound and re-minted per video (donor behavior).
                    val streamingPot = cache.get(session) ?: run {
                        val (fresh, expiresSec) = try {
                            m.mintStreamingPot(session)
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "poToken streaming mint failed: ${e.javaClass.simpleName}")
                            dropMinter()
                            return@withTimeout null
                        }
                        cache.put(session, fresh, expiresSec)
                        fresh
                    }
                    val playerPot = try {
                        m.mintPlayerPot(videoId)
                    } catch (_: Exception) {
                        streamingPot
                    }
                    com.howdy.echowave.domain.source.PoToken(playerPot, streamingPot)
                }
            }
        } catch (_: TimeoutCancellationException) {
            dropMinter()
            cache.clear()
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun dropMinter() {
        // No dispatcher switch here: BotGuardMinter.close() implementations
        // must be thread-safe themselves (BotGuardWebView posts to Main).
        try {
            minter?.close()
        } catch (_: Exception) {
        }
        minter = null
    }

    companion object {
        const val ANONYMOUS_SESSION = "anonymous"
        private const val PREWARM_ID = "prewarm"
        private const val TAG = "EchoWavePoToken"
    }
}

/**
 * Single-session BotGuard WebView. Mirrors the donor sequence:
 * asset html -> jnn/Create -> runBotGuard -> jnn/GenerateIT ->
 * createPoTokenMinter (once) -> obtainPoToken per identifier.
 */
class BotGuardWebView private constructor(private val context: Context) : BotGuardMinter {
    private lateinit var webView: WebView
    private var minterReady = false
    private val pending = java.util.concurrent.ConcurrentHashMap<String, Continuation<String>>()
    private var initCont: Continuation<BotGuardWebView>? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun mintStreamingPot(sessionId: String): Pair<String, Long> {
        // Streaming pot = first token minted in this session; its expiry
        // comes from the integrity token parsed at init. Re-mint once and
        // reuse the init expiry (donor: expirationInstant - 10 min margin,
        // enforced by PoTokenCache on the provider side).
        val pot = mint(sessionId)
        return pot to INIT_EXPIRY_SEC
    }

    override suspend fun mintPlayerPot(videoId: String): String = mint(videoId)

    override fun close() {
        runOnMain {
            runCatching {
                webView.clearHistory()
                webView.clearCache(true)
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            }
        }
        pending.forEach { (_, c) -> c.resumeWithException(PoTokenException("closed")) }
        pending.clear()
    }

    private suspend fun mint(identifier: String): String =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { cont ->
                pending[identifier] = cont
                webView.evaluateJavascript(
                    """try {
                        identifier = "$identifier"
                        u8Identifier = ${JsCodec.stringToU8(identifier)}
                        obtainPoToken(u8Identifier).then(function(poTokenU8) {
                            $BRIDGE.onObtainPoTokenResult(identifier, poTokenU8.join(","))
                        }).catch(function(error) {
                            $BRIDGE.onObtainPoTokenError(identifier, "" + error)
                        })
                    } catch (error) {
                        $BRIDGE.onObtainPoTokenError(identifier, "" + error)
                    }""",
                    null,
                )
            }
        }

    @JavascriptInterface
    fun downloadAndRunBotguard() {
        android.util.Log.d(TAG, "BotGuard challenge starting")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = jnn("https://www.youtube.com/api/jnn/v1/Create", "[ \"$REQUEST_KEY\" ]")
                    ?: return@launch failInit("Create: empty body")
                val challenge = JsCodec.parseChallengeData(body)
                eval(
                    """try {
                        data = $challenge
                        runBotGuard(data).then(function (result) {
                            this.webPoSignalOutput = result.webPoSignalOutput
                            $BRIDGE.onRunBotguardResult(result.botguardResponse)
                        }, function (error) {
                            $BRIDGE.onJsError("" + error)
                        })
                    } catch (error) { $BRIDGE.onJsError("" + error) }""",
                )
            } catch (e: Exception) {
                failInit("Create failed: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = jnn(
                    "https://www.youtube.com/api/jnn/v1/GenerateIT",
                    "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]",
                ) ?: return@launch failInit("GenerateIT: empty body")
                val (integrityU8, _) = JsCodec.parseIntegrityTokenData(body)
                eval(
                    """try {
                        this.integrityToken = $integrityU8
                        createPoTokenMinter(webPoSignalOutput, integrityToken).then(function() {
                            $BRIDGE.onMinterCreated()
                        }).catch(function(error) { $BRIDGE.onJsError("" + error) })
                    } catch (error) { $BRIDGE.onJsError("" + error) }""",
                )
            } catch (e: Exception) {
                failInit("GenerateIT failed: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun onMinterCreated() {
        minterReady = true
        initCont?.resume(this@BotGuardWebView)
        initCont = null
    }

    @JavascriptInterface
    fun onObtainPoTokenResult(identifier: String, commaSeparated: String) {
        val cont = pending.remove(identifier) ?: return
        try {
            cont.resume(JsCodec.u8ToBase64(commaSeparated))
        } catch (t: Throwable) {
            cont.resumeWithException(t)
        }
    }

    @JavascriptInterface
    fun onObtainPoTokenError(identifier: String, error: String) {
        pending.remove(identifier)?.resumeWithException(PoTokenException(error.take(300)))
    }

    @JavascriptInterface
    fun onJsError(error: String) {
        failInit(error.take(300))
    }

    private fun failInit(reason: String) {
        android.util.Log.e(TAG, "BotGuard init failed: $reason")
        initCont?.resumeWithException(PoTokenException(reason))
        initCont = null
    }

    private fun jnn(url: String, jsonBody: String): String? {
        val req = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json+protobuf".toMediaType()))
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json+protobuf")
            .header("x-goog-api-key", GOOGLE_API_KEY)
            .header("x-user-agent", "grpc-web-javascript/0.1")
            .build()
        http.newCall(req).execute().use { resp ->
            if (resp.code != 200) {
                android.util.Log.e(TAG, "jnn HTTP ${resp.code}")
                return null
            }
            return resp.body?.string()
        }
    }

    private fun eval(js: String) {
        runOnMain { webView.evaluateJavascript(js, null) }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else Handler(Looper.getMainLooper()).post(block)
    }

    companion object {
        private const val TAG = "EchoWavePoToken"
        private const val BRIDGE = "EchoWavePoToken"
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val INIT_EXPIRY_SEC = 6 * 60 * 60L

        suspend fun open(context: Context): BotGuardWebView {
            return withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { cont ->
                    val w = BotGuardWebView(context.applicationContext)
                    w.initCont = cont
                    try {
                        android.util.Log.d(TAG, "BotGuard WebView creating")
                        val web = WebView(context.applicationContext)
                        val s = web.settings
                        s.javaScriptEnabled = true
                        s.userAgentString = UA
                        s.blockNetworkLoads = true
                        web.addJavascriptInterface(w, BRIDGE)
                        web.webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                                if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                    android.util.Log.e(TAG, "BotGuard WebView JavaScript error")
                                }
                                return super.onConsoleMessage(m)
                            }
                        }
                        w.webView = web
                        android.util.Log.d(TAG, "BotGuard asset loading")
                        val html = context.applicationContext.assets.open("po_token.html")
                            .bufferedReader().use { it.readText() }
                        android.util.Log.d(TAG, "BotGuard asset loaded bytes=${html.length}")
                        val boot = html.replaceFirst(
                            "</script>",
                            "\n$BRIDGE.downloadAndRunBotguard()</script>",
                        )
                        web.loadDataWithBaseURL("https://www.youtube.com", boot, "text/html", "utf-8", null)
                    } catch (e: Exception) {
                        w.failInit("open failed: ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            }
        }
    }
}
