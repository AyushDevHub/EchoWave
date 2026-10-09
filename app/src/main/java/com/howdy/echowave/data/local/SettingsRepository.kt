package com.howdy.echowave.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class Appearance { SYSTEM, DARK, LIGHT }

enum class PlayerStyle(val label: String, val description: String) {
    CURRENT("Classic", "The default player design"),
    APPLE("Glow", "Artwork atmosphere with word-timed sing-along"),
    SPOTIFY("Contrast", "Bold lyrics with a clean, high-contrast player"),
}

enum class FontChoice(val label: String) {
    SYSTEM("System default"),
    GOOGLE_SANS("Google Sans (system)"),
    SANS_FLEX("Sans Flex (system)"),
    OUTFIT("Outfit"),
    JAKARTA("Plus Jakarta Sans"),
}

enum class ThemePreset(val label: String, val primary: Long, val background: Long, val surface: Long, val text: Long) {
    PURPLE("Purple", 0xFFE3B8FF, 0xFF09090D, 0xFF17151D, 0xFFF8F7FC),
    BEIGE_COPPER("Beige copper", 0xFFE0A77E, 0xFF17120F, 0xFF28201B, 0xFFF4E9DE),
    DARK_WHITE("Dark white", 0xFFE8E8ED, 0xFF08090C, 0xFF17191E, 0xFFF5F5F7),
    MINT("Mint", 0xFF8EE5C1, 0xFF07120F, 0xFF12211B, 0xFFE8FFF5),
    DARK_CRIMSON("Dark crimson", 0xFFFF7188, 0xFF12080C, 0xFF241117, 0xFFFFF0F2),
    DARK_BANANA("Dark banana", 0xFFF2D36F, 0xFF121108, 0xFF242114, 0xFFFFFBE8),
    CHOCOLATE_ICE("Chocolate & ice", 0xFFEBCDAE, 0xFF17100D, 0xFF2A201B, 0xFFFFF7EF),
    MIDNIGHT_AZURE("Midnight azure", 0xFF83BFFF, 0xFF07101B, 0xFF111F30, 0xFFEAF4FF),
    GEM("Gem", 0xFF72E0D3, 0xFF071313, 0xFF102322, 0xFFE8FFFC),
    NOTHING_RED("Nothing red", 0xFFFF465A, 0xFF090909, 0xFF1B1B1B, 0xFFF7F7F7),
    EMBER_GRAIN("Ember grain", 0xFFE4A452, 0xFF15110A, 0xFF292216, 0xFFFFF4DD),
    TEAL_MIST("Matte teal mist", 0xFF9BCBC5, 0xFF0B1212, 0xFF192323, 0xFFEAF5F3),
}

/** Tiny settings store. Grows only via explicit keys — no stringly soup in UI. */
class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val appearance: Flow<Appearance> =
        store.data.map { prefs ->
            when (prefs[APPEARANCE]) {
                "dark" -> Appearance.DARK
                "light" -> Appearance.LIGHT
                else -> Appearance.SYSTEM
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

    val themePreset: Flow<ThemePreset> = store.data.map { prefs ->
        ThemePreset.entries.firstOrNull { it.name == prefs[THEME_PRESET] } ?: ThemePreset.PURPLE
    }
    val displayName: Flow<String> = store.data.map { it[DISPLAY_NAME].orEmpty() }
    val greetingEnabled: Flow<Boolean> = store.data.map { it[GREETING_ENABLED] ?: true }
    val musicPreferences: Flow<String> = store.data.map { it[MUSIC_PREFERENCES].orEmpty() }
    /** True once the first-start onboarding (name + taste) has been completed. */
    val onboardingCompleted: Flow<Boolean> = store.data.map { it[ONBOARDING_COMPLETED] ?: false }
    val playerStyle: Flow<PlayerStyle> = store.data.map { prefs ->
        PlayerStyle.entries.firstOrNull { it.name == prefs[PLAYER_STYLE] } ?: PlayerStyle.APPLE
    }
    val fontChoice: Flow<FontChoice> = store.data.map { prefs ->
        FontChoice.entries.firstOrNull { it.name == prefs[FONT_CHOICE] } ?: FontChoice.SYSTEM
    }

    suspend fun setThemePreset(value: ThemePreset) { store.edit { it[THEME_PRESET] = value.name } }
    suspend fun setPlayerStyle(value: PlayerStyle) { store.edit { it[PLAYER_STYLE] = value.name } }
    suspend fun setFontChoice(value: FontChoice) { store.edit { it[FONT_CHOICE] = value.name } }
    suspend fun setDisplayName(value: String) { store.edit { it[DISPLAY_NAME] = value.trim().take(40) } }
    suspend fun setGreetingEnabled(value: Boolean) { store.edit { it[GREETING_ENABLED] = value } }
    suspend fun setMusicPreferences(value: String) { store.edit { it[MUSIC_PREFERENCES] = value.trim().take(120) } }
    suspend fun setOnboardingCompleted(value: Boolean) { store.edit { it[ONBOARDING_COMPLETED] = value } }

    companion object {
        const val GITHUB_URL = "https://github.com/AyushDevHub/EchoWave"
        const val CREDITS_URL = "https://github.com/AyushDevHub/EchoWave/blob/main/CREDITS.md"
        private val APPEARANCE = stringPreferencesKey("appearance")
        private val THEME_PRESET = stringPreferencesKey("theme_preset")
        private val FONT_CHOICE = stringPreferencesKey("font_choice")
        private val PLAYER_STYLE = stringPreferencesKey("player_style")
        private val DISPLAY_NAME = stringPreferencesKey("display_name")
        private val GREETING_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("greeting_enabled")
        private val MUSIC_PREFERENCES = stringPreferencesKey("music_preferences")
        private val ONBOARDING_COMPLETED = androidx.datastore.preferences.core.booleanPreferencesKey("onboarding_completed")
    }
}
