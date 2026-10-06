package com.howdy.echowave.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SearchHistoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(): SearchHistoryRepository {
        val file = java.io.File(tmp.root, "h-${System.nanoTime()}.preferences_pb")
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ) { file }
        return SearchHistoryRepository(store)
    }

    @Test fun `newest first deduped capped at ten`() = runBlocking {
        val repo = repo()
        assertEquals(emptyList<String>(), repo.recent.first())
        repeat(12) { repo.save("q$it") }
        val got = repo.recent.first()
        assertEquals(10, got.size)
        assertEquals("q11", got.first())
        assertEquals("q2", got.last())
        repo.save("q5")
        val moved = repo.recent.first()
        assertEquals("q5", moved.first())
        assertEquals(10, moved.size)
        repo.clear()
        assertEquals(emptyList<String>(), repo.recent.first())
    }

    @Test fun `blank never saved`() = runBlocking {
        val repo = repo()
        repo.save("   ")
        assertEquals(emptyList<String>(), repo.recent.first())
    }
}
