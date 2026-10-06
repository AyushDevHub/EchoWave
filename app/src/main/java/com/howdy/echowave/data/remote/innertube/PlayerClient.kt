package com.howdy.echowave.data.remote.innertube

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

/**
 * The identity a googlevideo URL says minted it, as far as its media fetch
 * has to repeat it. Port of Echo-Music's utils/PlayerClient.kt
 * (GPL-3.0, see CREDITS.md). Pure logic + unit tests; no playback changes.
 */
data class PlayerClient(
    val clientName: String,
    val clientVersion: String,
    val userAgent: String,
    val origin: String? = null,
) {
    val referer: String?
        get() = origin?.let { "$it/" }

    /** Headers the media request must carry for a URL this client minted. */
    fun mediaHeaders(): Map<String, String> = buildMap {
        put("User-Agent", userAgent)
        origin?.let { put("Origin", it) }
        referer?.let { put("Referer", it) }
    }

    companion object {
        private const val MUSIC_ORIGIN = "https://music.youtube.com"
        private const val YOUTUBE_ORIGIN = "https://www.youtube.com"

        private const val WEB_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

        val IOS = PlayerClient(
            "IOS", "21.26.4",
            "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
        )
        val ANDROID = PlayerClient(
            "ANDROID", "21.26.364",
            "com.google.android.youtube/21.26.364 " +
                "(Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002; Cronet/132.0.6834.79) gzip",
        )
        val ANDROID_MUSIC = PlayerClient(
            "ANDROID_MUSIC", "8.39.42",
            "com.google.android.apps.youtube.music/8.39.42 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002) gzip",
        )
        val ANDROID_VR = PlayerClient(
            "ANDROID_VR", "1.65.10",
            "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
        )
        val WEB_REMIX = PlayerClient("WEB_REMIX", "1.20260707.12.00", WEB_UA, MUSIC_ORIGIN)
        val WEB = PlayerClient("WEB", "2.20260708.00.00", WEB_UA, YOUTUBE_ORIGIN)
        val TVHTML5 = PlayerClient(
            "TVHTML5", "7.20260707.07.00",
            "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15",
            YOUTUBE_ORIGIN,
        )
        val VISIONOS = PlayerClient(
            "VISIONOS", "1.02",
            "com.google.ios.youtube/1.02 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/605.1.15 (KHTML, like Gecko)",
        )

        /** Client a stream URL names via c/cver; falls back to IOS when unknown. */
        fun forStreamUrl(url: String): PlayerClient {
            val parsed = url.toHttpUrlOrNull() ?: return IOS
            val name = parsed.queryParameter("c")?.uppercase(Locale.ROOT) ?: return IOS
            return when {
                name.startsWith("IOS") -> IOS
                name == "VISIONOS" -> VISIONOS
                name == "ANDROID_MUSIC" -> ANDROID_MUSIC
                // Check the specialized Android clients before the broad
                // ANDROID prefix so media fetches retain the minting identity.
                name == "ANDROID_VR" -> ANDROID_VR
                name.startsWith("ANDROID") -> ANDROID
                name.startsWith("TVHTML5") -> TVHTML5
                name == "WEB_REMIX" -> WEB_REMIX
                name.startsWith("WEB") || name == "MWEB" -> WEB
                else -> IOS
            }
        }

        /** Largest single range googlevideo reliably serves for this URL's client. */
        fun rangeBytesFor(url: String): Long {
            val parsed = url.toHttpUrlOrNull() ?: return Long.MAX_VALUE
            if (!parsed.host.endsWith("googlevideo.com")) return Long.MAX_VALUE
            val name = parsed.queryParameter("c")?.uppercase(Locale.ROOT)
            return if (name == "ANDROID_VR" || name?.startsWith("TVHTML5_SIMPLY") == true) {
                NARROW_RANGE_BYTES
            } else {
                RANGE_BYTES
            }
        }

        private const val RANGE_BYTES = 1024L * 1024
        private const val NARROW_RANGE_BYTES = 512L * 1024
    }
}
