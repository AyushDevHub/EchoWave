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
        // Non-existent path on purpose: a pre-created EMPTY file trips
        // DataStore init on some versions; missing file is always clean.
        val file = java.io.File(tmp.root, "prefs-${System.nanoTime()}.preferences_pb")
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ) { file }
        return SettingsRepository(store)
    }

    @Test fun `defaults to system then persists dark`() = runBlocking {
        val file = java.io.File(tmp.root, "prefs-${System.nanoTime()}.preferences_pb")
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ) { file }
        val repo = SettingsRepository(store)
        assertEquals(Appearance.SYSTEM, repo.appearance.first())
        repo.setAppearance(Appearance.DARK)
        assertEquals(Appearance.DARK, repo.appearance.first())
        repo.setAppearance(Appearance.LIGHT)
        assertEquals(Appearance.LIGHT, repo.appearance.first())
    }

    @Test fun `persists theme greeting name and taste preferences`() = runBlocking {
        val repository = repo()
        assertEquals(ThemePreset.PURPLE, repository.themePreset.first())
        assertEquals(true, repository.greetingEnabled.first())
        repository.setThemePreset(ThemePreset.MIDNIGHT_AZURE)
        repository.setDisplayName("  Asta  ")
        repository.setGreetingEnabled(false)
        repository.setMusicPreferences("Hindi, Bhojpuri, Indie")
        assertEquals(ThemePreset.MIDNIGHT_AZURE, repository.themePreset.first())
        assertEquals("Asta", repository.displayName.first())
        assertEquals(false, repository.greetingEnabled.first())
        assertEquals("Hindi, Bhojpuri, Indie", repository.musicPreferences.first())
    }
}
