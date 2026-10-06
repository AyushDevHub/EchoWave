package com.howdy.echowave

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.howdy.echowave.data.local.EchoWaveDb
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.data.remote.fallback.BravePipeFallbackResolver
import com.howdy.echowave.data.remote.fallback.NewPipeFallbackResolver
import com.howdy.echowave.data.remote.innertube.InnerTubeMusicSource
import com.howdy.echowave.data.remote.innertube.InnerTubeStreamResolver
import com.howdy.echowave.data.remote.innertube.StreamProbe
import com.howdy.echowave.data.remote.innertube.StreamRegistry
import com.howdy.echowave.data.remote.innertube.VisitorBootstrap
import com.howdy.echowave.data.remote.innertube.VisitorStore
import com.howdy.echowave.data.remote.innertube.buildInnerTubeApi
import com.howdy.echowave.data.remote.lyrics.LyricsRepository
import com.howdy.echowave.data.remote.potoken.WebViewPoTokenProvider
import com.howdy.echowave.data.repository.LibraryRepositoryImpl
import com.howdy.echowave.playback.PlaybackSessionConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.howdy.echowave.data.repository.MusicRepositoryImpl
import com.howdy.echowave.domain.source.ChainedStreamResolver
import com.howdy.echowave.features.ai.NoOpRecommendationProvider
import com.howdy.echowave.playback.Media3PlaybackController

/**
 * Manual composition root. Single swap point for sources/resolvers.
 * Real InnerTube is now default; fakes remain for offline UI work.
 */
class AppContainer(ctx: Context) {
    private val appCtx = ctx.applicationContext

    val db: EchoWaveDb by lazy {
        // Dev-phase schema churn (v1->v2 playlists): destructive rebuild is
        // acceptable pre-release; real migrations start at v1.0 ship.
        Room.databaseBuilder(appCtx, EchoWaveDb::class.java, "echowave.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val prefs by lazy {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(SharedPreferencesMigration(appCtx, "echowave_prefs")),
        ) { appCtx.preferencesDataStoreFile("echowave_prefs") }
    }

    private val innerTubeApi by lazy {
        buildInnerTubeApi(debug = false, visitor = { visitorStore.current() })
    }

    val musicSource by lazy { InnerTubeMusicSource(innerTubeApi, visitorStore) }

    /** visitorData echoed by InnerTube; bound into requests + token sessions. */
    val visitorStore by lazy { VisitorStore.sharedPrefs(appCtx) }

    val streamRegistry by lazy { StreamRegistry() }

    private val probe by lazy { StreamProbe() }

    /** Local WebView BotGuard minting (Echo-Music approach). Null-safe: failures -> no token. */
    val poTokens by lazy {
        WebViewPoTokenProvider(
            appCtx,
            sessionOf = { visitorStore.current() ?: WebViewPoTokenProvider.ANONYMOUS_SESSION },
        ).also { it.prewarm() }
    }

    val streamResolver by lazy {
        ChainedStreamResolver(
            listOf(
                InnerTubeStreamResolver(innerTubeApi, poTokens, visitorStore, probe, streamRegistry),
                NewPipeFallbackResolver(),
                BravePipeFallbackResolver(),
            ),
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Eager startup work. MUST be called from Application.onCreate —
     * leaving this lazy is what made the first tap pay the 20 s cold
     * tax (WebView BotGuard + visitor bootstrap on the play path).
     */
    fun startup() {
        scope.launch {
            if (visitorStore.current() == null) {
                VisitorBootstrap.fetch()?.let { visitorStore.offer(it) }
            }
        }
        poTokens.prewarm()
    }

    val musicRepo by lazy { MusicRepositoryImpl(musicSource, streamResolver) }
    val libraryRepo by lazy { LibraryRepositoryImpl(db.trackDao(), db.playlistDao()) }
    val lyricsRepo by lazy { LyricsRepository() }
    val settingsRepo by lazy { SettingsRepository(prefs) }
    val recommendations by lazy { NoOpRecommendationProvider() }

    val playback by lazy { Media3PlaybackController(musicRepo, libraryRepo) }

    /**
     * Single app-lifetime session connection. Activity recreation reuses it
     * (connect is idempotent); it is intentionally never released on
     * backgrounding — release would deafen notification/lock-screen and
     * end-of-track advance while audio continues in the service.
     */
    val sessionConnector by lazy { PlaybackSessionConnector(appCtx, playback) }
}
