package com.howdy.echowave.domain.model

/**
 * InnerTube search filter chips. Param strings verified against
 * Echo-Music `YouTube.SearchFilter` (GPL-3.0 donor, see CREDITS.md) —
 * passed verbatim as the `params` field of the search body.
 */
enum class SearchFilter(val params: String) {
    SONGS("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"),
    VIDEOS("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D"),
    ALBUMS("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D"),
    ARTISTS("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D"),
}
