package com.howdy.echowave.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Appearance { SYSTEM, DARK, LIGHT }

/** Tiny settings store. Grows only via explicit keys — no stringly soup in UI. */
class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val appearance: Flow<Appearance> =
        store.data.map { prefs ->
            when (prefs[APPEARANCE]) {
                "dark" -> Appearance.DARK
                "light" -> Appearance.LIGHT
                else -> Appearance.DARK
            }
        }

    suspend fun setAppearance(value: Appearance) {
        store.edit { prefs ->
            prefs[APPEARANCE] = when (value) {
                Appearance.DARK -> "dark"
                Appearance.LIGHT -> "light"
                Appearance.SYSTEM -> "system"
            }
        }
    }

    companion object {
        const val GITHUB_URL = "https://github.com/AyushDevHub/EchoWave"
        private val APPEARANCE = stringPreferencesKey("appearance")
    }
}
