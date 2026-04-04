package ai.shieldtv.app.history

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaType

data class WatchHistoryItem(
    val mediaType: MediaType,
    val ids: MediaIds,
    val title: String,
    val year: Int? = null,
    val artworkUrl: String? = null,
    val subtitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val watchedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun stableKey(): String {
        val base = listOfNotNull(
            mediaType.name,
            ids.tmdbId?.let { "tmdb:$it" },
            ids.imdbId?.let { "imdb:$it" },
            ids.tvdbId?.let { "tvdb:$it" },
            title.ifBlank { null }
        )
        return if (mediaType == MediaType.SHOW) {
            base.plus(
                listOfNotNull(
                    seasonNumber?.let { "season:$it" },
                    episodeNumber?.let { "episode:$it" }
                )
            ).joinToString("|")
        } else {
            base.joinToString("|")
        }
    }
}
