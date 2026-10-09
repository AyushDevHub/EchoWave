package com.howdy.echowave.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val artworkUrl: String?,
    val durationMs: Long?,
    val favorite: Boolean = false,
    val lastPlayedAt: Long? = null,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("playlistId"), Index("trackId")],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val playlistId: Long,
    val trackId: String,
    val position: Int,
    val title: String,
    val artist: String,
    val album: String?,
    val artworkUrl: String?,
    val durationMs: Long?,
)

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val trackCount: Int,
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE favorite = 1 ORDER BY title")
    fun favorites(): Flow<List<TrackEntity>>

    @Query("SELECT id FROM tracks WHERE favorite = 1")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun history(limit: Int): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun get(id: String): TrackEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM tracks WHERE id = :id AND favorite = 1)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrackEntity)

    @Query("UPDATE tracks SET favorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("UPDATE tracks SET lastPlayedAt = :ts WHERE id = :id")
    suspend fun touchPlayed(id: String, ts: Long)

    @Query("UPDATE tracks SET favorite = NOT favorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)
}

@Dao
interface PlaylistDao {
    @Insert
    suspend fun createPlaylist(entity: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: String)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun contains(playlistId: Long, trackId: String): Int

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun count(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrack(entity: PlaylistTrackEntity)

    @Transaction
    suspend fun addIfAbsent(
        playlistId: Long,
        trackId: String,
        title: String,
        artist: String,
        album: String?,
        artworkUrl: String?,
        durationMs: Long?,
    ) {
        if (contains(playlistId, trackId) > 0) return
        val pos = count(playlistId)
        addTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = pos,
                title = title,
                artist = artist,
                album = album,
                artworkUrl = artworkUrl,
                durationMs = durationMs,
            ),
        )
    }

    @Transaction
    @Query("SELECT p.*, COUNT(t.trackId) AS trackCount FROM playlists p LEFT JOIN playlist_tracks t ON t.playlistId = p.id GROUP BY p.id ORDER BY p.createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>
}

@Database(
    entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, ListeningEventEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class EchoWaveDb : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun listeningEventDao(): ListeningEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `playlists` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `playlist_tracks` (" +
                        "`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`playlistId` INTEGER NOT NULL, " +
                        "`trackId` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`artist` TEXT NOT NULL, " +
                        "`album` TEXT, " +
                        "`artworkUrl` TEXT, " +
                        "`durationMs` INTEGER, " +
                        "FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_playlist_tracks_playlistId` ON `playlist_tracks` (`playlistId`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_playlist_tracks_trackId` ON `playlist_tracks` (`trackId`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `listening_events` (" +
                        "`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`trackId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`artist` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`playedAt` INTEGER NOT NULL, " +
                        "`listenMs` INTEGER NOT NULL, " +
                        "`completionRatio` REAL, " +
                        "`hourOfDay` INTEGER NOT NULL, " +
                        "`context` TEXT NOT NULL, " +
                        "`durationMs` INTEGER)"
                )
            }
        }
    }
}
