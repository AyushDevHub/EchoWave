package com.howdy.echowave.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(): SettingsRepository {
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ) { tmp.newFile("prefs.preferences_pb") }
        return SettingsRepository(store)
    }

    @Test fun `defaults to system then persists dark`() = runBlocking {
        val repo = repo()
        assertEquals(Appearance.SYSTEM, repo.appearance.first())
        repo.setAppearance(Appearance.DARK)
        assertEquals(Appearance.DARK, repo.appearance.first())
        repo.setAppearance(Appearance.LIGHT)
        assertEquals(Appearance.LIGHT, repo.appearance.first())
    }
}
