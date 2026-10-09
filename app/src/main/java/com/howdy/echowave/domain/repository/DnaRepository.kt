package com.howdy.echowave.domain.repository

import com.howdy.echowave.domain.dna.ListeningEvent
import com.howdy.echowave.domain.dna.MusicDnaProfile
import kotlinx.coroutines.flow.Flow

/**
 * Capture seam for playback. Playback depends only on this interface —
 * never on Room, DataStore, or AI — keeping the AI boundary intact.
 */
interface ListeningEventRecorder {
    suspend fun record(event: ListeningEvent)
}

/**
 * Music DNA store: raw events (12-month TTL) -> decayed aggregate snapshot.
 * Consumers (Echo This, recommendations) observe the profile, never raw rows.
 */
interface DnaRepository : ListeningEventRecorder {
    fun observeProfile(): Flow<MusicDnaProfile>
    suspend fun currentProfile(): MusicDnaProfile
    fun observeEnabled(): Flow<Boolean>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun recompute()
    suspend fun prune()
    suspend fun clearAll()
}
