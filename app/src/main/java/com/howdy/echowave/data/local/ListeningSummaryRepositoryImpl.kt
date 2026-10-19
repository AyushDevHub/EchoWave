package com.howdy.echowave.data.local

import com.howdy.echowave.domain.dna.ListeningEvent
import com.howdy.echowave.domain.dna.ListeningEventType
import com.howdy.echowave.domain.dna.PlayContext
import com.howdy.echowave.domain.model.ListeningSummary
import com.howdy.echowave.domain.model.ListeningSummaryPeriod
import com.howdy.echowave.domain.repository.ListeningSummaryRepository
import com.howdy.echowave.features.listening.ListeningSummaryAggregator
import java.time.ZoneId

class ListeningSummaryRepositoryImpl(
    private val events: ListeningEventDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ListeningSummaryRepository {
    override suspend fun load(
        period: ListeningSummaryPeriod,
        includeDayParts: Boolean,
    ): ListeningSummary {
        val now = clock()
        val rows = events.eventsSince(now - ListeningSummaryAggregator.RETAINED_HISTORY_MS)
        return ListeningSummaryAggregator.aggregate(
            events = rows.map(ListeningEventEntity::toDomain),
            period = period,
            now = now,
            zone = zone(),
            includeDayParts = includeDayParts,
        )
    }
}

private fun ListeningEventEntity.toDomain() = ListeningEvent(
    trackId = trackId,
    title = title,
    artist = artist,
    type = runCatching { ListeningEventType.valueOf(type) }.getOrDefault(ListeningEventType.START),
    playedAt = playedAt,
    listenMs = listenMs,
    completionRatio = completionRatio,
    hourOfDay = hourOfDay,
    context = runCatching { PlayContext.valueOf(context) }.getOrDefault(PlayContext.UNKNOWN),
    durationMs = durationMs,
)
