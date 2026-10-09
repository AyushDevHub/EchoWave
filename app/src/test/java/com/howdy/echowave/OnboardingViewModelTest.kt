package com.howdy.echowave.ui.onboarding

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.domain.model.SearchItem
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.StreamInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private class StubMusic(
    private val artists: List<SearchItem.Artist> = listOf(
        SearchItem.Artist("a1", "Arijit Singh", null),
    ),
) : MusicRepository {
    override suspend fun search(query: String): AppResult<SearchResults> =
        AppResult.Ok(SearchResults(items = artists))
    override suspend fun resolveStream(trackId: String): AppResult<StreamInfo> =
        AppResult.Err("stub")
}

class OnboardingViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun settings(): SettingsRepository {
        val file = java.io.File(tmp.root, "prefs-${System.nanoTime()}.preferences_pb")
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ) { file }
        return SettingsRepository(store)
    }

    private fun vm(music: MusicRepository = StubMusic(), settings: SettingsRepository): OnboardingViewModel =
        OnboardingViewModel(music, settings, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))

    @Test fun `blank name blocks continue`() {
        val vm = vm(settings = settings())
        assertFalse(vm.ui.value.canContinue())
        vm.next()
        assertEquals(0, vm.ui.value.step)
    }

    @Test fun `name then genre advance steps`() {
        val vm = vm(settings = settings())
        vm.onName("  Asta  ")
        assertTrue(vm.ui.value.canContinue())
        vm.next()
        assertEquals(1, vm.ui.value.step)
        assertFalse(vm.ui.value.canContinue())
        vm.toggleGenre("Pop")
        assertTrue(vm.ui.value.canContinue())
        vm.next()
        assertEquals(2, vm.ui.value.step)
    }

    @Test fun `genre toggle is reversible`() {
        val vm = vm(settings = settings())
        vm.toggleGenre("Pop")
        vm.toggleGenre("Pop")
        assertTrue(vm.ui.value.genres.isEmpty())
    }

    @Test fun `artist search fills results and toggle selects`() = runBlocking {
        val vm = vm(settings = settings())
        vm.onArtistQuery("arijit")
        delay(800)
        assertEquals(listOf("a1"), vm.ui.value.artistResults.map { it.id })
        vm.toggleArtist(vm.ui.value.artistResults.first())
        assertTrue(vm.isSelected(SearchItem.Artist("a1", "Arijit Singh", null)))
        assertEquals("Arijit Singh", vm.ui.value.tasteQuery())
    }

    @Test fun `taste query combines artists and genres`() {
        val vm = vm(settings = settings())
        vm.toggleGenre("Pop")
        vm.toggleArtist(SearchItem.Artist("a1", "Arijit Singh", null))
        assertEquals("Arijit Singh, Pop", vm.ui.value.tasteQuery())
    }

    @Test fun `complete persists name taste and flag`() = runBlocking {
        val repo = settings()
        val vm = vm(settings = repo)
        vm.onName("Asta")
        vm.toggleGenre("Chill")
        var done = false
        vm.complete(editMode = false) { done = true }
        delay(800)
        assertTrue(done)
        assertEquals("Asta", repo.displayName.first())
        assertEquals("Chill", repo.musicPreferences.first())
        assertTrue(repo.onboardingCompleted.first())
    }

    @Test fun `edit mode does not reset completed flag`() = runBlocking {
        val repo = settings()
        repo.setOnboardingCompleted(true)
        val vm = vm(settings = repo)
        vm.onName("Asta")
        var done = false
        vm.complete(editMode = true) { done = true }
        delay(800)
        assertTrue(done)
        assertTrue(repo.onboardingCompleted.first())
    }

    @Test fun `blank name never saves`() = runBlocking {
        val repo = settings()
        val vm = vm(settings = repo)
        var done = false
        vm.complete(editMode = false) { done = true }
        delay(300)
        assertFalse(done)
        assertFalse(repo.onboardingCompleted.first())
    }
}
