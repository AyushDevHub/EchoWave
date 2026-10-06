package com.howdy.echowave.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
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

@Database(entities = [TrackEntity::class], version = 1, exportSchema = false)
abstract class EchoWaveDb : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}
