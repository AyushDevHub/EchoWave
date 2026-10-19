package com.howdy.echowave

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.howdy.echowave.data.dna.DnaRepositoryImpl
import com.howdy.echowave.data.local.ListeningEventDao
import com.howdy.echowave.data.local.ListeningEventEntity
import com.howdy.echowave.domain.dna.ListeningEvent
import com.howdy.echowave.domain.dna.ListeningEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DnaRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `disabling hides profile and stops collection while reset deletes stored data`() = runBlocking {
        val events = FakeListeningEventDao()
        val prefsFile = java.io.File(temporaryFolder.root, "dna-${System.nanoTime()}.preferences_pb")
        val prefs = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ) { prefsFile }
        val repository = DnaRepositoryImpl(events, prefs, clock = { 1_800_000_000_000L })

        repository.record(event("track-1", ListeningEventType.COMPLETE))
        repository.recompute()
        assertEquals(1, repository.currentProfile().totalEvents)

        repository.setEnabled(false)
        assertEquals(false, repository.observeEnabled().first())
        assertEquals(0, repository.currentProfile().totalEvents)
        assertEquals(0, repository.observeProfile().first().totalEvents)

        repository.record(event("track-2", ListeningEventType.COMPLETE))
        assertEquals(1, events.rows.size)

        repository.setEnabled(true)
        assertEquals(1, repository.currentProfile().totalEvents)
        repository.clearAll()
        assertEquals(0, repository.currentProfile().totalEvents)
        assertEquals(0, events.rows.size)
    }

    private fun event(trackId: String, type: ListeningEventType) = ListeningEvent(
        trackId = trackId,
        title = "Song $trackId",
        artist = "Example Artist",
        type = type,
        playedAt = 1_800_000_000_000L,
        listenMs = 180_000L,
        completionRatio = 1f,
        durationMs = 180_000L,
    )
}

private class FakeListeningEventDao : ListeningEventDao {
    val rows = mutableListOf<ListeningEventEntity>()

    override suspend fun insert(entity: ListeningEventEntity) {
        rows += entity.copy(rowId = (rows.size + 1).toLong())
    }

    override suspend fun eventsSince(since: Long): List<ListeningEventEntity> =
        rows.filter { it.playedAt >= since }

    override suspend fun countSince(since: Long): Int = rows.count { it.playedAt >= since }

    override suspend fun prune(before: Long): Int {
        val oldCount = rows.size
        rows.removeAll { it.playedAt < before }
        return oldCount - rows.size
    }

    override suspend fun clear() {
        rows.clear()
    }
}
