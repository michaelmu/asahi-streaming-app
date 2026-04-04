package ai.shieldtv.app.favorites

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaType

data class FavoriteItem(
    val mediaType: MediaType,
    val ids: MediaIds,
    val title: String,
    val year: Int? = null,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    val addedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun stableKey(): String {
        return listOfNotNull(
            mediaType.name,
            ids.tmdbId?.let { "tmdb:$it" },
            ids.imdbId?.let { "imdb:$it" },
            ids.tvdbId?.let { "tvdb:$it" },
            title.ifBlank { null }
        ).joinToString("|")
    }
}
