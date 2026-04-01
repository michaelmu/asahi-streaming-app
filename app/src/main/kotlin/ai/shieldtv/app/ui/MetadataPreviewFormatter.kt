package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.media.SearchResult

object MetadataPreviewFormatter {
    fun describe(searchResults: List<SearchResult>): String {
        val looksLive = searchResults.any { result ->
            result.posterUrl != null ||
                !(result.badges.contains("stub")) ||
                (result.mediaRef.ids.tmdbId?.all { it.isDigit() } == true)
        }

        return buildString {
            appendLine("TMDb Mode: ${if (looksLive) "live-ish" else "fallback"}")
            appendLine("Search Results: ${searchResults.size}")
            searchResults.forEachIndexed { index, result ->
                appendLine(
                    "${index + 1}. ${result.mediaRef.title} (${result.subtitle ?: "no subtitle"}) " +
                        "[tmdb=${result.mediaRef.ids.tmdbId ?: "none"}, year=${result.mediaRef.year ?: "n/a"}, poster=${if (result.posterUrl != null) "yes" else "no"}]"
                )
            }
        }
    }
}
