package com.howdy.echowave.features.listening

import com.howdy.echowave.domain.dna.ListeningEvent
import com.howdy.echowave.domain.dna.ListeningEventType
import com.howdy.echowave.domain.model.ListeningSummary
import com.howdy.echowave.domain.model.ListeningSummaryDayPart
import com.howdy.echowave.domain.model.ListeningSummaryItem
import com.howdy.echowave.domain.model.ListeningSummaryPeriod
import com.howdy.echowave.features.dna.LanguageHeuristic
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Aggregates only locally stored playback outcomes; it does not infer genres or upload data. */
object ListeningSummaryAggregator {
    const val RETAINED_HISTORY_MS = 365L * 24 * 60 * 60 * 1000
    private const val PLAY_THRESHOLD_MS = 30_000L
    private val outcomeTypes = setOf(
        ListeningEventType.COMPLETE,
        ListeningEventType.SKIP_EARLY,
        ListeningEventType.SKIP_LATE,
        ListeningEventType.REPEAT,
    )

    fun aggregate(
        events: List<ListeningEvent>,
        period: ListeningSummaryPeriod,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        includeDayParts: Boolean = false,
    ): ListeningSummary {
        val retainedSince = now - RETAINED_HISTORY_MS
        val periodStart = periodStart(period, now, zone).coerceAtLeast(retainedSince)
        val outcomes = events.asSequence()
            .filter { it.type in outcomeTypes && it.playedAt in periodStart..now }
            .toList()
        val countedPlays = outcomes.filter(::countsAsPlay)
        val trackCounts = countedPlays.groupingBy { it.trackId }.eachCount()
        val artistCounts = countedPlays
            .filter { it.artist.isNotBlank() && !it.artist.equals("Unknown artist", ignoreCase = true) }
            .groupingBy { normalized(it.artist) }
            .eachCount()
        val trackLabels = countedPlays.associateBy({ it.trackId }, { it.title })
        val artistLabels = countedPlays.associateBy({ normalized(it.artist) }, { it.artist.trim() })
        val languageCounts = countedPlays.mapNotNull { event ->
            LanguageHeuristic.guessLanguage(event.title, event.artist)?.let { it to event }
        }.groupingBy { it.first }.eachCount()

        val allRetainedOutcomes = events.filter {
            it.type in outcomeTypes && it.playedAt in retainedSince..now && countsAsPlay(it)
        }
        val firstSeenByArtist = allRetainedOutcomes
            .filter { it.artist.isNotBlank() && !it.artist.equals("Unknown artist", ignoreCase = true) }
            .groupBy { normalized(it.artist) }
            .mapValues { (_, listens) -> listens.minOf { it.playedAt } }
        val newArtistKeys = firstSeenByArtist
            .filterValues { it in periodStart..now }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }

        val dayPartTimes = if (includeDayParts) {
            outcomes.filter(::countsAsPlay).groupBy { dayPart(it.hourOfDay) }
                .map { (part, listens) ->
                    ListeningSummaryDayPart(part, listens.sumOf { it.listenMs.coerceAtLeast(0L) })
                }.sortedBy { dayPartOrder(it.label) }
        } else {
            emptyList()
        }

        return ListeningSummary(
            period = period,
            listeningMs = outcomes.sumOf { it.listenMs.coerceAtLeast(0L) },
            plays = countedPlays.size,
            completedTracks = outcomes.count { it.type == ListeningEventType.COMPLETE },
            uniqueTracks = trackCounts.size,
            uniqueArtists = artistCounts.size,
            topArtists = artistCounts.toSummaryItems(artistLabels).take(5),
            topTracks = trackCounts.toSummaryItems(trackLabels).take(5),
            repeatedTracks = trackCounts.filterValues { it >= 2 }.toSummaryItems(trackLabels).take(5),
            languageHints = languageCounts.map { (language, count) ->
                ListeningSummaryItem(language, language, count)
            }.sortedWith(compareByDescending<ListeningSummaryItem> { it.count }.thenBy { it.label }).take(5),
            newlyExploredArtists = newArtistKeys.mapNotNull { key ->
                firstSeenByArtist[key]?.let { firstSeen ->
                    val label = countedPlays.firstOrNull { normalized(it.artist) == key }?.artist?.trim() ?: key
                    ListeningSummaryItem(key, label, artistCounts[key] ?: 0).takeIf { firstSeen in periodStart..now }
                }
            },
            listeningByDayPart = dayPartTimes,
            hasListeningEvents = outcomes.isNotEmpty(),
        )
    }

    fun periodStart(period: ListeningSummaryPeriod, now: Long, zone: ZoneId): Long {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val startDate = when (period) {
            ListeningSummaryPeriod.TODAY -> today
            ListeningSummaryPeriod.THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ListeningSummaryPeriod.THIS_MONTH -> today.withDayOfMonth(1)
            ListeningSummaryPeriod.ALL_RECORDED -> return now - RETAINED_HISTORY_MS
        }
        return startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun countsAsPlay(event: ListeningEvent): Boolean =
        event.type == ListeningEventType.COMPLETE ||
            event.type == ListeningEventType.REPEAT ||
            event.listenMs >= PLAY_THRESHOLD_MS

    private fun normalized(value: String): String = value.trim().lowercase(Locale.ROOT)

    private fun Map<String, Int>.toSummaryItems(labels: Map<String, String>) =
        map { (key, count) -> ListeningSummaryItem(key, labels[key] ?: key, count) }
            .sortedWith(compareByDescending<ListeningSummaryItem> { it.count }.thenBy { it.label.lowercase(Locale.ROOT) })

    private fun dayPart(hour: Int): String = when (hour) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..20 -> "Evening"
        in 0..23 -> "Night"
        else -> "Unknown time"
    }

    private fun dayPartOrder(value: String): Int = when (value) {
        "Morning" -> 0
        "Afternoon" -> 1
        "Evening" -> 2
        "Night" -> 3
        else -> 4
    }
}
