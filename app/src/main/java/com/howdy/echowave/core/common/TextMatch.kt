package com.howdy.echowave.core.common

import java.text.Normalizer
import java.util.Locale

/**
 * Script-agnostic title/artist matching for search ranking.
 *
 * Ported from LastWave-native `app/.../data/music/TextMatch.kt` (GPL-3.0,
 * see CREDITS.md). The scoring shape is theirs; the candidate signature
 * takes plain strings instead of their track type so any layer can use it.
 * Pure Kotlin, no Android — unit-testable.
 */
object TextMatch {
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
    private val DIACRITICS = Regex("\\p{M}+")
    private val MULTI_SPACE = Regex("\\s+")
    val VARIANT_WORDS = setOf(
        "live", "remix", "karaoke", "cover", "instrumental", "slowed", "sped", "nightcore",
        "acoustic", "demo", "edit", "remaster", "remastered", "mono", "stereo",
        "version", "deluxe", "bonus", "mix", "extended", "radio", "clean", "explicit",
        "original", "orchestral", "unplugged", "rerecorded", "anniversary", "edition",
    )
    val MATCH_NOISE_WORDS = setOf(
        "official", "audio", "video", "visualizer", "lyrics", "lyric",
        "hd", "hq", "4k", "track", "music", "full",
    )
    private val FEATURING_CLAUSE = Regex("(?i)[(\\[]\\s*(feat(?:uring)?|ft)\\.?\\s+.*?[)\\]]")
    private val VERSION_OR_LABEL_CLAUSE =
        Regex("(?i)[(\\[][^)\\]]*(official|music\\s*video|audio|video|visualizer|lyrics?|hd|hq|4k|live|remix|acoustic|demo|edit|remaster(?:ed)?|mono|stereo|deluxe|bonus|version|mix)[^)\\]]*[)\\]]")

    fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .replace(NON_WORD, " ")
            .trim()
            .replace(MULTI_SPACE, " ")

    fun tokens(value: String): Set<String> = normalize(value)
        .split(' ')
        .filter { it.isNotBlank() && it !in MATCH_NOISE_WORDS }
        .toSet()

    fun baseTitle(value: String): String = value
        .replace(FEATURING_CLAUSE, " ")
        .replace(VERSION_OR_LABEL_CLAUSE, " ")

    fun similarity(a: String, b: String): Int {
        val normA = normalize(a)
        val normB = normalize(b)
        if (normA == normB) return 100
        if (normA.isBlank() || normB.isBlank()) return 0
        if (normA.contains(normB) || normB.contains(normA)) {
            val ratio = (minOf(normA.length, normB.length) * 100) / maxOf(normA.length, normB.length)
            if (ratio >= 75) return maxOf(85, ratio)
        }
        val left = tokens(a)
        val right = tokens(b)
        if (left.isEmpty() || right.isEmpty()) return 0
        val common = left.intersect(right).size
        val dice = (200 * common) / (left.size + right.size)
        val extraWords = (left + right) - left.intersect(right)
        val isSafeSubset = common > 0 && common == minOf(left.size, right.size) &&
            extraWords.all { word -> word in VARIANT_WORDS || word in MATCH_NOISE_WORDS || word.all(Char::isDigit) }
        val subset = if (isSafeSubset) 80 else 0
        return maxOf(dice, subset)
    }

    fun isSafeTitleMatch(candidateTitle: String, wantedTitle: String, artist: String = ""): Boolean {
        val normWanted = normalize(wantedTitle)
        val normCandidate = normalize(candidateTitle)
        if (normWanted == normCandidate) return true
        if (normWanted.isBlank() || normCandidate.isBlank()) return false

        val cleanCandidate = if (artist.isNotBlank()) {
            val normArtist = normalize(artist)
            if (normArtist.isNotBlank() && normCandidate.startsWith(normArtist)) {
                normCandidate.removePrefix(normArtist).trim().removePrefix("-").trim()
            } else normCandidate
        } else normCandidate

        if (cleanCandidate == normWanted) return true

        val baseWanted = normalize(baseTitle(wantedTitle))
        val baseCandidate = normalize(baseTitle(cleanCandidate))
        if (baseWanted.isNotBlank() && baseWanted == baseCandidate) return true

        val wantedTokens = tokens(wantedTitle)
        val candidateTokens = tokens(cleanCandidate)
        if (wantedTokens.isEmpty() || candidateTokens.isEmpty()) return false

        val common = wantedTokens.intersect(candidateTokens)
        if (common.size == wantedTokens.size || common.size == candidateTokens.size) {
            val extraWords = (wantedTokens + candidateTokens) - common
            val artistTokens = if (artist.isNotBlank()) tokens(artist) else emptySet()
            val allAllowedNoise = VARIANT_WORDS + MATCH_NOISE_WORDS + artistTokens
            if (extraWords.all { it in allAllowedNoise || it.all(Char::isDigit) }) {
                return true
            }
        }

        val sim = maxOf(
            similarity(cleanCandidate, wantedTitle),
            similarity(baseCandidate, baseWanted),
        )
        return sim >= 65
    }

    fun matchScore(candidateTitle: String, candidateArtist: String, title: String, artist: String): Int {
        val wantedTitle = normalize(title)
        val wantedArtist = normalize(artist)
        val candidateTitle = normalize(candidateTitle)
        val candidateArtist = normalize(candidateArtist)
        val titleSim = maxOf(
            similarity(candidateTitle, title),
            similarity(baseTitle(candidateTitle), baseTitle(title)),
        )
        val artistSim = similarity(candidateArtist, artist)
        var score = titleSim * 5 + artistSim * 3
        if (candidateTitle == wantedTitle) score += 600
        if (wantedArtist.isNotBlank() && candidateArtist == wantedArtist) score += 350
        val wantedVariants = tokens(title).intersect(VARIANT_WORDS)
        val unexpectedVariants = tokens(candidateTitle).intersect(VARIANT_WORDS) - wantedVariants
        score -= unexpectedVariants.size * 250
        if (wantedArtist.isNotBlank()) {
            val artistContains = candidateArtist.contains(wantedArtist) || wantedArtist.contains(candidateArtist)
            val titleContainsArtist = candidateTitle.contains(wantedArtist)
            if (artistSim < 35 && !artistContains && !titleContainsArtist) {
                score -= 1000
            }
        }
        if (titleSim < 50) {
            score -= 1500
        }
        return score
    }
}
