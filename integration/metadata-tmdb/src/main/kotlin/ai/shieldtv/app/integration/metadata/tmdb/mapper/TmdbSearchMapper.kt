package ai.shieldtv.app.integration.metadata.tmdb.mapper

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult

class TmdbSearchMapper {
    fun fromQueryEcho(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        return listOf(
            SearchResult(
                mediaRef = MediaRef(
                    mediaType = MediaType.MOVIE,
                    ids = MediaIds(tmdbId = "tmdb-${query.lowercase()}", imdbId = null, tvdbId = null),
                    title = query.replaceFirstChar { it.uppercase() },
                    year = 2024
                ),
                subtitle = "Movie • Placeholder TMDb result",
                posterUrl = null,
                backdropUrl = null,
                badges = listOf("movie", "stub")
            ),
            SearchResult(
                mediaRef = MediaRef(
                    mediaType = MediaType.SHOW,
                    ids = MediaIds(tmdbId = "tmdb-show-${query.lowercase()}", imdbId = null, tvdbId = null),
                    title = "$query Series",
                    year = 2023
                ),
                subtitle = "Show • Placeholder TMDb result",
                posterUrl = null,
                backdropUrl = null,
                badges = listOf("show", "stub")
            )
        )
    }
}
