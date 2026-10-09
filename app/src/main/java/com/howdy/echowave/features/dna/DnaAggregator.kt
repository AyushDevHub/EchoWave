package com.howdy.echowave.features.dna

import com.howdy.echowave.domain.dna.DnaStats
import com.howdy.echowave.domain.dna.ListeningEvent
import com.howdy.echowave.domain.dna.ListeningEventType
import com.howdy.echowave.domain.dna.MusicDnaProfile
import com.howdy.echowave.domain.dna.ScoredItem
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.exp
import kotlin.math.ln

/**
 * Pure taste aggregation: events -> decayed profile.
 * No Android, no Room, no network — fully unit-testable.
 *
 * Weights: repeat/favorite/playlist-add reward, early-skip penalizes.
 * Decay: exponential half-life so taste shifts (e.g. Bollywood -> Indie)
 * emerge without keeping every old row forever.
 */
object DnaAggregator {
    const val HALF_LIFE_DAYS = 75.0
    private val LAMBDA = ln(2.0) / HALF_LIFE_DAYS
    private const val DAY_MS = 86_400_000.0

    fun baseWeight(type: ListeningEventType): Double = when (type) {
        ListeningEventType.COMPLETE -> 1.0
        ListeningEventType.REPEAT -> 3.0
        ListeningEventType.START -> 0.3
        ListeningEventType.SKIP_EARLY -> -2.0
        ListeningEventType.SKIP_LATE -> -0.5
        ListeningEventType.FAVORITE -> 2.0
        ListeningEventType.PLAYLIST_ADD -> 2.0
    }

    fun decayedWeight(type: ListeningEventType, ageMs: Long): Double {
        val ageDays = (ageMs.coerceAtLeast(0L)) / DAY_MS
        return baseWeight(type) * exp(-LAMBDA * ageDays)
    }

    fun aggregate(events: List<ListeningEvent>, now: Long): MusicDnaProfile {
        if (events.isEmpty()) return MusicDnaProfile.empty(now)
        val artistScores = HashMap<String, Agg>()
        val langScores = HashMap<String, Agg>()
        val daypartScores = HashMap<String, Double>()
        var completes = 0
        var repeats = 0
        var early = 0
        var late = 0
        var favs = 0
        var adds = 0

        for (e in events) {
            val w = decayedWeight(e.type, now - e.playedAt)
            when (e.type) {
                ListeningEventType.COMPLETE -> completes++
                ListeningEventType.REPEAT -> repeats++
                ListeningEventType.SKIP_EARLY -> early++
                ListeningEventType.SKIP_LATE -> late++
                ListeningEventType.FAVORITE -> favs++
                ListeningEventType.PLAYLIST_ADD -> adds++
                ListeningEventType.START -> Unit
            }
            val artistKey = e.artist.trim().lowercase(java.util.Locale.ROOT).take(120)
            if (artistKey.isNotBlank() && artistKey != "unknown artist") {
                val a = artistScores.getOrPut(artistKey) { Agg(e.artist.trim().take(120)) }
                a.score += w
                a.count++
            }
            // Use keyword fallback too so transliterated Hindi (no Indic script)
            // still counts toward DNA language, matching the recommender.
            val langGuess = LanguageHeuristic.guessLanguage(e.title, e.artist)
                ?: com.howdy.echowave.domain.recommendation.TrackFeatureExtractor
                    .extract(
                        com.howdy.echowave.domain.model.Track(
                            id = e.trackId, title = e.title, artist = e.artist,
                        ),
                    ).language?.replaceFirstChar { it.uppercase() }
            langGuess?.let { lang ->
                val l = langScores.getOrPut(lang) { Agg(lang) }
                l.score += w
                l.count++
            }
            val bucket = DayPart.of(e.hourOfDay)
            daypartScores[bucket] = (daypartScores[bucket] ?: 0.0) + maxOf(w, 0.0)
        }

        val topArtists = artistScores.entries
            .filter { it.value.score > 0 }
            .sortedByDescending { it.value.score }
            .take(10)
            .map { ScoredItem(it.key, it.value.label, round2(it.value.score), it.value.count) }
        val languages = langScores.entries
            .filter { it.value.score > 0 }
            .sortedByDescending { it.value.score }
            .take(5)
            .map { ScoredItem(it.key, it.value.label, round2(it.value.score), it.value.count) }
        val dayTotal = daypartScores.values.sum()
        val dayparts = if (dayTotal > 0) {
            daypartScores.mapValues { round2(it.value / dayTotal) }
        } else {
            emptyMap()
        }
        val best = dayparts.maxByOrNull { it.value }?.key
        return MusicDnaProfile(
            updatedAt = now,
            totalEvents = events.size,
            topArtists = topArtists,
            topGenres = emptyList(),
            languages = languages,
            dayparts = dayparts,
            stats = DnaStats(events.size, completes, repeats, early, late, favs, adds),
            bestListeningTime = best?.let(::prettyDaypart),
            currentVibe = vibeLabel(repeats, completes, early, late, best, events.size),
        )
    }

    private fun prettyDaypart(bucket: String): String = when (bucket) {
        DayPart.MORNING -> "Morning"
        DayPart.AFTERNOON -> "Afternoon"
        DayPart.EVENING -> "Evening"
        else -> "Late Night"
    }

    internal fun vibeLabel(
        repeats: Int,
        completes: Int,
        early: Int,
        late: Int,
        best: String?,
        total: Int,
    ): String {
        if (total < 5) return "Getting to know you"
        if (repeats >= 5 && repeats > completes / 2) return "Repeat lover"
        if (best == DayPart.LATE_NIGHT) return "Late-night listener"
        if (early > completes) return "Selective explorer"
        if (completes >= total / 2) return "Deep listener"
        return "Curious explorer"
    }

    /** Local-hour bucket for an epoch timestamp. Visible for tests. */
    fun hourBucketOf(playedAt: Long): Int {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = playedAt
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun round2(v: Double): Double = kotlin.math.round(v * 100) / 100.0

    private class Agg(var label: String, var score: Double = 0.0, var count: Int = 0)
}
