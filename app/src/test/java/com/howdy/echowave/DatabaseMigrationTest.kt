package com.howdy.echowave.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {
    @Test
    fun `migration 1 to 2 executes expected SQL statements`() {
        assertEquals(1, EchoWaveDb.MIGRATION_1_2.startVersion)
        assertEquals(2, EchoWaveDb.MIGRATION_1_2.endVersion)

        val executedSql = mutableListOf<String>()
        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        EchoWaveDb.MIGRATION_1_2.migrate(fakeDb)

        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `playlists`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `playlist_tracks`") })
        assertTrue(executedSql.any { it.contains("index_playlist_tracks_playlistId") })
    }

    @Test
    fun `migration 2 to 3 executes expected SQL statements`() {
        assertEquals(2, EchoWaveDb.MIGRATION_2_3.startVersion)
        assertEquals(3, EchoWaveDb.MIGRATION_2_3.endVersion)

        val executedSql = mutableListOf<String>()
        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        EchoWaveDb.MIGRATION_2_3.migrate(fakeDb)

        assertTrue(executedSql.any { it.contains("index_playlist_tracks_trackId") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `listening_events`") })
    }
}
