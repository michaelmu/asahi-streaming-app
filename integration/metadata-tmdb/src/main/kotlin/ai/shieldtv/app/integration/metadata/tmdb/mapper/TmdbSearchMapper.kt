package ai.shieldtv.app.integration.metadata.tmdb.mapper

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import org.json.JSONObject

class TmdbSearchMapper {
    fun fromQueryEcho(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val trimmed = query.trim()
        if (!trimmed.startsWith("{")) {
            return placeholderResults(trimmed)
        }

        return runCatching {
            val root = JSONObject(trimmed)
            val results = root.optJSONArray("results") ?: return@runCatching emptyList<SearchResult>()
            val mapped = mutableListOf<SearchResult>()
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val mediaType = when (item.optString("media_type")) {
                    "movie" -> MediaType.MOVIE
                    "tv" -> MediaType.SHOW
                    else -> null
                } ?: continue

                val title = when (mediaType) {
                    MediaType.MOVIE -> item.optString("title")
                    MediaType.SHOW -> item.optString("name")
                    else -> ""
                }
                if (title.isBlank()) continue

                val year = when (mediaType) {
                    MediaType.MOVIE -> item.optString("release_date")
                    MediaType.SHOW -> item.optString("first_air_date")
                    else -> ""
                }.take(4).toIntOrNull()

                mapped += SearchResult(
                    mediaRef = MediaRef(
                        mediaType = mediaType,
                        ids = MediaIds(
                            tmdbId = item.opt("id")?.toString(),
                            imdbId = null,
                            tvdbId = null
                        ),
                        title = title,
                        year = year
                    ),
                    subtitle = when (mediaType) {
                        MediaType.MOVIE -> "Movie"
                        MediaType.SHOW -> "Show"
                        else -> null
                    },
                    posterUrl = item.optString("poster_path").takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w500$it" },
                    backdropUrl = item.optString("backdrop_path").takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w780$it" },
                    badges = listOf(mediaType.name.lowercase())
                )
            }
            mapped
        }.getOrElse {
            placeholderResults(trimmed)
        }
    }

    private fun placeholderResults(query: String): List<SearchResult> {
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
