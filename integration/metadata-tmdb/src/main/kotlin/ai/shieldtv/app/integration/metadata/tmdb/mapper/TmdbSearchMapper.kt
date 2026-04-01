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
                    ids = MediaIds(tmdbId = null, imdbId = null, tvdbId = null),
                    title = query,
                    year = null
                ),
                subtitle = "Placeholder TMDb result",
                posterUrl = null,
                backdropUrl = null,
                badges = listOf("stub")
            )
        )
    }
}
