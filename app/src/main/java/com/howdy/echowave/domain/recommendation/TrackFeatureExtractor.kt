package com.howdy.echowave.domain.recommendation

import com.howdy.echowave.core.common.TextMatch
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.features.dna.LanguageHeuristic
import java.util.Locale

enum class TrackStyle {
    DANCE_PARTY,
    ROMANTIC_MELODIC,
    ACOUSTIC_CHILL,
    DEVOTIONAL,
    SOUNDTRACK,
    REMIX_CLUB,
}

data class ExtractedTrackFeatures(
    val language: String?,
    val era: String?,
    val styles: Set<TrackStyle>,
    val baseTitleTokens: Set<String>,
    val normalizedArtist: String?,
)

object TrackFeatureExtractor {
    private val DANCE_KEYWORDS = setOf(
        "dance", "party", "club", "dj", "bhangra", "dhol", "beat", "nach", "jalwa",
        "disco", "edm", "dhamaal", "groove", "thumka", "bass", "tapori", "item", "item song",
        "beedi", "sheila", "jawani", "dance number",
    )

    private val ROMANTIC_KEYWORDS = setOf(
        "love", "romantic", "dil", "pyar", "ishq", "mohabbat", "humsafar", "sanam",
        "tum", "tere", "meri", "slowed", "reverb", "sad", "dard", "judaai", "soul",
    )

    private val ACOUSTIC_KEYWORDS = setOf(
        "acoustic", "unplugged", "lofi", "lo-fi", "chill", "piano", "guitar", "soft",
        "reprise", "stripped", "ambient", "meditation",
    )

    private val DEVOTIONAL_KEYWORDS = setOf(
        "bhajan", "aarti", "chalisa", "kirtan", "mantra", "shiv", "krishna", "ram",
        "hanuman", "ganesh", "devotional", "stotram", "bhakti",
    )

    private val SOUNDTRACK_KEYWORDS = setOf(
        "soundtrack", "ost", "theme", "score", "motion picture", "film", "movie",
    )

    private val REMIX_KEYWORDS = setOf(
        "remix", "mix", "mashup", "club mix", "extended", "reworked",
    )

    private val YEAR_REGEX = Regex("""\b(19[5-9]\d|20[0-3]\d)\b""")

    fun extract(track: Track): ExtractedTrackFeatures {
        val titleLower = track.title.lowercase(Locale.ROOT)
        val artistLower = track.artist.lowercase(Locale.ROOT)
        val albumLower = track.album?.lowercase(Locale.ROOT) ?: ""
        val combined = "$titleLower $artistLower $albumLower"
        val wordTokens = combined.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()

        val lang = LanguageHeuristic.guessLanguage(track.title, track.artist)?.lowercase(Locale.ROOT)
            ?: inferLanguageFromKeywords(combined, wordTokens)

        val era = when {
            hasToken(wordTokens, "90s") || combined.contains("90's") -> "1990s"
            hasToken(wordTokens, "80s") || combined.contains("80's") -> "1980s"
            hasToken(wordTokens, "70s") || combined.contains("70's") -> "1970s"
            hasToken(wordTokens, "2000s") || hasToken(wordTokens, "00s") -> "2000s"
            hasToken(wordTokens, "retro") || combined.contains("golden era") -> "retro"
            else -> {
                val match = YEAR_REGEX.find(combined)?.value?.toIntOrNull()
                match?.let { yearToDecade(it) }
            }
        }

        val styles = mutableSetOf<TrackStyle>()
        if (matchesKeyword(combined, wordTokens, DANCE_KEYWORDS)) styles.add(TrackStyle.DANCE_PARTY)
        if (matchesKeyword(combined, wordTokens, ROMANTIC_KEYWORDS)) styles.add(TrackStyle.ROMANTIC_MELODIC)
        if (matchesKeyword(combined, wordTokens, ACOUSTIC_KEYWORDS)) styles.add(TrackStyle.ACOUSTIC_CHILL)
        if (matchesKeyword(combined, wordTokens, DEVOTIONAL_KEYWORDS)) styles.add(TrackStyle.DEVOTIONAL)
        if (matchesKeyword(combined, wordTokens, SOUNDTRACK_KEYWORDS)) styles.add(TrackStyle.SOUNDTRACK)
        if (matchesKeyword(combined, wordTokens, REMIX_KEYWORDS)) styles.add(TrackStyle.REMIX_CLUB)

        val baseTitle = TextMatch.normalize(TextMatch.baseTitle(track.title))
        val tokens = baseTitle.split(' ')
            .filter { it.isNotBlank() && it !in TextMatch.MATCH_NOISE_WORDS }
            .toSet()

        val normArtist = track.artist.takeUnless {
            it.isBlank() || it.equals("Unknown artist", ignoreCase = true)
        }?.let(TextMatch::normalize)

        return ExtractedTrackFeatures(
            language = lang,
            era = era,
            styles = styles,
            baseTitleTokens = tokens,
            normalizedArtist = normArtist,
        )
    }

    private fun yearToDecade(year: Int): String = when (year) {
        in 1950..1969 -> "classic"
        in 1970..1979 -> "1970s"
        in 1980..1989 -> "1980s"
        in 1990..1999 -> "1990s"
        in 2000..2009 -> "2000s"
        in 2010..2019 -> "2010s"
        in 2020..2029 -> "2020s"
        in 2030..2039 -> "2030s"
        else -> "other"
    }

    private fun hasToken(tokens: Set<String>, word: String): Boolean = word in tokens

    private fun matchesKeyword(combined: String, tokens: Set<String>, keywords: Set<String>): Boolean {
        for (kw in keywords) {
            val key = kw.lowercase(Locale.ROOT)
            if (' ' in key || '-' in key) {
                if (combined.contains(key)) return true
            } else if (key in tokens) {
                return true
            }
        }
        return false
    }

    private fun inferLanguageFromKeywords(combined: String, tokens: Set<String>): String? {
        // Phrase list matched with boundaries to avoid "ram" in "drama".
        fun hasAny(phrases: List<String>): Boolean = phrases.any { phrase ->
            if (' ' in phrase) combined.contains(phrase)
            else phrase.lowercase(Locale.ROOT) in tokens
        }
        return when {
            hasAny(listOf("arijit", "sunidhi", "shreya ghoshal", "sonu nigam", "alka yagnik", "kumar sanu", "udit narayan", "kishore", "lata", "rafi", "badshah", "honey singh", "neha kakkar", "jubin", "bollywood", "hindi", "beedi", "fevicol", "omkara", "dabangg", "sheila", "jawani", "dard e disco", "dard-e-disco", "tees maar khan", "om shanti om")) -> "hindi"
            hasAny(listOf("sidhu", "moosewala", "diljit", "dosanjh", "ap dhillon", "karan aujla", "bhangra", "punjabi")) -> "punjabi"
            hasAny(listOf("anirudh", "ar rahman", "ilayaraja", "yuvan", "harris jayaraj", "sid sriram", "tamil")) -> "tamil"
            hasAny(listOf("dsp", "devi sri", "thaman", "keeravaani", "telugu")) -> "telugu"
            hasAny(listOf("taylor swift", "drake", "ed sheeran", "billie eilish", "the weeknd", "ariana grande", "eminem", "coldplay")) -> "english"
            else -> null
        }
    }
}
