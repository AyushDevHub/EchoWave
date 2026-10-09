package com.howdy.echowave.data.remote.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

data class LatestAppRelease(
    val version: String,
    val releasePageUrl: String,
)

/** Reads public release metadata only; it never downloads or installs an APK. */
object GitHubReleaseChecker {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/AyushDevHub/EchoWave/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersion: String): LatestAppRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "EchoWave-Android/$currentVersion")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub release check returned HTTP ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("GitHub returned an empty release response")
            val release = JSONObject(body)
            val tag = release.optString("tag_name").trim()
            val latestVersion = tag.removePrefix("v")
            if (!isNewerVersion(latestVersion, currentVersion)) return@withContext null

            val expectedApk = "EchoWave-$latestVersion.apk"
            val assets = release.optJSONArray("assets")
            val hasApk = assets != null && (0 until assets.length()).any { index ->
                assets.optJSONObject(index)?.optString("name") == expectedApk
            }
            if (!hasApk) throw IOException("The latest release does not include its APK")

            val pageUrl = release.optString("html_url")
            val uri = runCatching { URI(pageUrl) }.getOrNull()
            if (uri?.scheme != "https" || uri.host != "github.com" ||
                !uri.path.orEmpty().startsWith("/AyushDevHub/EchoWave/releases/")
            ) {
                throw IOException("GitHub returned an invalid release page")
            }
            LatestAppRelease(latestVersion, uri.toASCIIString())
        }
    }

    internal fun isNewerVersion(latest: String, installed: String): Boolean {
        fun parts(value: String): List<Int>? {
            val numeric = value.trim().removePrefix("v").substringBefore('-').substringBefore('+')
            if (numeric.isEmpty()) return null
            val result = numeric.split('.').map { it.toIntOrNull() ?: return null }
            return result.takeIf { it.isNotEmpty() }
        }

        val latestParts = parts(latest) ?: return false
        val installedParts = parts(installed) ?: return false
        for (index in 0 until maxOf(latestParts.size, installedParts.size)) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val installedPart = installedParts.getOrElse(index) { 0 }
            if (latestPart != installedPart) return latestPart > installedPart
        }
        return false
    }
}
