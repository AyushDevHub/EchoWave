package com.howdy.echowave.data.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Process-wide 500MB LRU disk cache manager for ExoPlayer audio streams.
 * Repeated listens, loops, and seeking are served from disk with 0 network calls.
 */
@UnstableApi
object MediaCacheManager {
    const val MAX_CACHE_BYTES = 500L * 1024L * 1024L // 500 MB

    @Volatile
    private var cacheInstance: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return cacheInstance ?: run {
            val cacheDir = File(context.applicationContext.cacheDir, "media_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
            val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
            SimpleCache(cacheDir, evictor, dbProvider).also { cacheInstance = it }
        }
    }

    fun createCacheDataSourceFactory(
        context: Context,
        upstreamFactory: DataSource.Factory,
    ): CacheDataSource.Factory {
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
