package com.howdy.echowave.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Raw listening signals. 12-month TTL via [ListeningEventDao.prune];
 * long-term taste lives in the DNA snapshot, not here.
 */
@Entity(tableName = "listening_events")
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    /** ListeningEventType name. */
    val type: String,
    val playedAt: Long,
    val listenMs: Long,
    val completionRatio: Float?,
    val hourOfDay: Int,
    /** PlayContext name. */
    val context: String,
    val durationMs: Long?,
)

@Dao
interface ListeningEventDao {
    @Insert
    suspend fun insert(entity: ListeningEventEntity)

    @Query("SELECT * FROM listening_events WHERE playedAt >= :since ORDER BY playedAt ASC")
    suspend fun eventsSince(since: Long): List<ListeningEventEntity>

    @Query("SELECT COUNT(*) FROM listening_events WHERE playedAt >= :since")
    suspend fun countSince(since: Long): Int

    @Query("DELETE FROM listening_events WHERE playedAt < :before")
    suspend fun prune(before: Long): Int

    @Query("DELETE FROM listening_events")
    suspend fun clear()
}
