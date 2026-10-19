package com.howdy.echowave.domain.repository

import com.howdy.echowave.domain.model.ListeningSummary
import com.howdy.echowave.domain.model.ListeningSummaryPeriod

interface ListeningSummaryRepository {
    suspend fun load(
        period: ListeningSummaryPeriod,
        includeDayParts: Boolean,
    ): ListeningSummary
}
