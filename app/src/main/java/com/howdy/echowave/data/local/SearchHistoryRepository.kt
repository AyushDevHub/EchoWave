package com.howdy.echowave.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Last 10 successful search queries, newest first. */
class SearchHistoryRepository(
    private val store: DataStore<Preferences>,
) {
    val recent: Flow<List<String>> =
        store.data.map { prefs ->
            prefs[QUERIES]?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()
        }

    suspend fun save(query: String) {
        val clean = query.trim().take(80)
            .replace(SEP, "")
            .replace("\n", " ")
            .replace("\r", "")
            .trim()
        if (clean.isEmpty()) return
        store.edit { prefs ->
            val current = prefs[QUERIES]?.split(SEP)?.filter { it.isNotBlank() } ?: emptyList()
            prefs[QUERIES] = (listOf(clean) + current.filter { it != clean }).take(MAX).joinToString(SEP)
        }
    }

    suspend fun clear() {
        store.edit { prefs -> prefs.remove(QUERIES) }
    }

    companion object {
        const val MAX = 10
        internal const val SEP = ""
        private val QUERIES = stringPreferencesKey("recent_searches")
    }
}
