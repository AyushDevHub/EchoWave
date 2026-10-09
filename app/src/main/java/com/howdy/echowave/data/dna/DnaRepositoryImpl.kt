package com.howdy.echowave.data.dna

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.howdy.echowave.data.local.ListeningEventDao
import com.howdy.echowave.data.local.ListeningEventEntity
import com.howdy.echowave.domain.dna.ListeningEvent
import com.howdy.echowave.domain.dna.ListeningEventType
import com.howdy.echowave.domain.dna.MusicDnaProfile
import com.howdy.echowave.domain.dna.PlayContext
import com.howdy.echowave.domain.repository.DnaRepository
import com.howdy.echowave.features.dna.DnaAggregator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Room (raw, 12-mo TTL) + DataStore (snapshot + enabled flag).
 * Recompute is throttled: every 20 events or 24h, plus explicit calls.
 */
class DnaRepositoryImpl(
    private val events: ListeningEventDao,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val clock: () -> Long = System::currentTimeMillis,
) : DnaRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val recomputeMutex = Mutex()

    override fun observeProfile(): Flow<MusicDnaProfile> =
        prefs.data.map { decode(it[DNA_JSON]) }

    override suspend fun currentProfile(): MusicDnaProfile =
        decode(prefs.data.first()[DNA_JSON])

    override fun observeEnabled(): Flow<Boolean> =
        prefs.data.map { it[DNA_ENABLED] ?: true }

    override suspend fun setEnabled(enabled: Boolean) {
        prefs.edit { it[DNA_ENABLED] = enabled }
    }

    override suspend fun record(event: ListeningEvent) {
        if (prefs.data.first()[DNA_ENABLED] == false) return
        events.insert(event.toEntity())
        // Atomic increment via updateData: concurrent records can't lose counts.
        prefs.edit { prefs ->
            val since = prefs[EVENTS_SINCE_RECOMPUTE] ?: 0
            prefs[EVENTS_SINCE_RECOMPUTE] = since + 1
        }
        val snapshot = prefs.data.first()
        val next = snapshot[EVENTS_SINCE_RECOMPUTE] ?: 0
        val last = snapshot[DNA_UPDATED] ?: 0L
        if (next >= RECOMPUTE_EVERY || clock() - last > RECOMPUTE_MAX_STALE_MS) {
            scope.launch {
                runCatching { recompute() }
            }
        }
    }

    override suspend fun recompute() {
        recomputeMutex.withLock {
            if (prefs.data.first()[DNA_ENABLED] == false) return
            val now = clock()
            val rows = events.eventsSince(now - RETENTION_MS)
            val domain = rows.map { it.toDomain() }
            val profile = DnaAggregator.aggregate(domain, now)
            prefs.edit {
                it[DNA_JSON] = json.encodeToString(MusicDnaProfile.serializer(), profile)
                it[DNA_UPDATED] = now
                it[EVENTS_SINCE_RECOMPUTE] = 0
            }
        }
    }

    override suspend fun prune() {
        events.prune(clock() - RETENTION_MS)
    }

    override suspend fun clearAll() {
        events.clear()
        prefs.edit {
            it.remove(DNA_JSON)
            it.remove(DNA_UPDATED)
            it[EVENTS_SINCE_RECOMPUTE] = 0
        }
    }

    private fun decode(raw: String?): MusicDnaProfile {
        if (raw.isNullOrBlank()) return MusicDnaProfile.empty(clock())
        return runCatching { json.decodeFromString(MusicDnaProfile.serializer(), raw) }
            .getOrDefault(MusicDnaProfile.empty(clock()))
    }

    companion object {
        /** 12-month raw retention per v1 decision. */
        const val RETENTION_MS = 365L * 24 * 60 * 60 * 1000
        private const val RECOMPUTE_EVERY = 20
        private const val RECOMPUTE_MAX_STALE_MS = 24L * 60 * 60 * 1000
        private val DNA_JSON = stringPreferencesKey("dna_profile_json")
        private val DNA_UPDATED = longPreferencesKey("dna_updated_at")
        private val DNA_ENABLED = booleanPreferencesKey("dna_enabled")
        private val EVENTS_SINCE_RECOMPUTE = intPreferencesKey("dna_events_since_recompute")
    }
}

private fun ListeningEvent.toEntity() = ListeningEventEntity(
    trackId = trackId,
    title = title.take(200),
    artist = artist.take(200),
    type = type.name,
    playedAt = playedAt,
    listenMs = listenMs,
    completionRatio = completionRatio,
    hourOfDay = hourOfDay,
    context = context.name,
    durationMs = durationMs,
)

private fun ListeningEventEntity.toDomain() = ListeningEvent(
    trackId = trackId,
    title = title,
    artist = artist,
    type = runCatching { ListeningEventType.valueOf(type) }.getOrDefault(ListeningEventType.START),
    playedAt = playedAt,
    listenMs = listenMs,
    completionRatio = completionRatio,
    hourOfDay = hourOfDay,
    context = runCatching { PlayContext.valueOf(context) }.getOrDefault(PlayContext.UNKNOWN),
    durationMs = durationMs,
)
