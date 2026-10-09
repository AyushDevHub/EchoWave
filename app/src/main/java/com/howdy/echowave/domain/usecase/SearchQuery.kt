package com.howdy.echowave.domain.usecase

/** A title-only query or an explicit `title by artist` / `title - artist` query. */
data class SearchQuery(val title: String, val artist: String? = null) {
    companion object {
        private val ARTIST_SEPARATOR = Regex("(?i)^(.+?)\\s+(?:by|-)\\s+(.+)$")

        fun parse(raw: String): SearchQuery {
            val query = raw.trim()
            val match = ARTIST_SEPARATOR.matchEntire(query) ?: return SearchQuery(query)
            val title = match.groupValues[1].trim()
            val artist = match.groupValues[2].trim()
            return if (title.isBlank() || artist.isBlank()) SearchQuery(query) else SearchQuery(title, artist)
        }
    }
}
