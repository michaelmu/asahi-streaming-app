package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.media.TitleDetails

object DetailsPreviewFormatter {
    fun describe(details: TitleDetails?): String {
        if (details == null) return "Selected Details:\nnone"

        val looksLive = details.posterUrl != null || details.backdropUrl != null || details.genres.isNotEmpty()

        return buildString {
            appendLine("Selected Details:")
            appendLine("Mode: ${if (looksLive) "live-ish" else "fallback"}")
            appendLine("Title: ${details.mediaRef.title}")
            appendLine("Year: ${details.mediaRef.year ?: "n/a"}")
            appendLine("TMDb ID: ${details.mediaRef.ids.tmdbId ?: "none"}")
            appendLine("IMDb ID: ${details.mediaRef.ids.imdbId ?: "none"}")
            appendLine("Genres: ${if (details.genres.isNotEmpty()) details.genres.joinToString() else "none"}")
            appendLine("Runtime: ${details.runtimeMinutes ?: "n/a"} min")
            appendLine("Poster: ${if (details.posterUrl != null) "yes" else "no"}")
            appendLine("Backdrop: ${if (details.backdropUrl != null) "yes" else "no"}")
            appendLine("Overview: ${details.overview ?: "none"}")
        }
    }
}
