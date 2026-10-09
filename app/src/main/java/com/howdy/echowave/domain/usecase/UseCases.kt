package com.howdy.echowave.domain.usecase

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.core.common.TextMatch
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.repository.MusicRepository
import com.howdy.echowave.domain.source.StreamInfo

class SearchTracksUseCase(
    private val repo: MusicRepository,
) {
    suspend operator fun invoke(query: String): AppResult<SearchResults> {
        val q = query.trim()
        if (q.isBlank()) return AppResult.Ok(SearchResults.empty())
        val base = when (val r = repo.search(q)) {
            is AppResult.Ok -> r.value
            is AppResult.Err -> return r
        }
        return AppResult.Ok(SearchResultRanker.rank(base, q))
    }
}

/** Strong title resemblance ("3 idiot" matches "3 Idiots", "love" does not match "Love Songs Deluxe"). */
internal fun queryMatchesTitle(title: String, query: String): Boolean {
    val normalizedTitle = TextMatch.normalize(title)
    val normalizedQuery = TextMatch.normalize(query)
    if (normalizedTitle.isEmpty() || normalizedQuery.isEmpty()) return false
    if (TextMatch.similarity(title, query) >= 85) return true

    // Movie albums often add a descriptor after the film name, and users
    // commonly omit the final plural ("3 idiot" -> "3 Idiots"). Match the
    // complete query prefix after singularizing ordinary trailing s's.
    // Guard against "james/glass/news" -> "jame/glas/new": never strip
    // -ss, -us, -is endings.
    val suffixWords = setOf("album", "film", "movie", "song", "soundtrack", "original", "motion", "picture", "ost", "deluxe", "live", "remix", "version", "edition")
    fun canonicalWords(value: String) = TextMatch.tokens(value).map { word ->
        if (word.length > 3 && word.endsWith('s') &&
            !word.endsWith("ss") && !word.endsWith("us") && !word.endsWith("is")
        ) word.dropLast(1) else word
    }
    val wanted = canonicalWords(normalizedQuery)
    val candidate = canonicalWords(normalizedTitle)
    if (wanted.isEmpty() || candidate.size < wanted.size) return false
    if (candidate.take(wanted.size) == wanted) {
        val trailing = candidate.drop(wanted.size)
        return trailing.all { it in suffixWords }
    }
    return TextMatch.similarity(title, query) >= 75
}

class ResolveStreamUseCase(
    private val repo: MusicRepository,
) {
    suspend operator fun invoke(trackId: String): AppResult<StreamInfo> =
        repo.resolveStream(trackId)
}
