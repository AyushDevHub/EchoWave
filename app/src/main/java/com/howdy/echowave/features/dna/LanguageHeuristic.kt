package com.howdy.echowave.features.dna

/**
 * Script-based language hint from title/artist text.
 * Conservative: Latin script returns null (unknown) rather than guessing
 * English — transliterated Bengali/Hindi titles must not be mislabeled.
 */
object LanguageHeuristic {
    fun guessLanguage(title: String, artist: String): String? {
        val text = "$title $artist"
        for (ch in text) {
            when (ch.code) {
                in 0x0980..0x09FF -> return "Bengali"
                in 0x0900..0x097F -> return "Hindi"
                in 0x0B80..0x0BFF -> return "Tamil"
                in 0x0C00..0x0C7F -> return "Telugu"
                in 0x0C80..0x0CFF -> return "Kannada"
                in 0x0D00..0x0D7F -> return "Malayalam"
                in 0x0A00..0x0A7F -> return "Punjabi"
                in 0x0A80..0x0AFF -> return "Gujarati"
            }
        }
        return null
    }
}
