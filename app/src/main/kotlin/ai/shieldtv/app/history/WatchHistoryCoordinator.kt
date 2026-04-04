package ai.shieldtv.app.history

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.playback.PlaybackItem

class WatchHistoryCoordinator(
    private val store: WatchHistoryStore
) {
    fun list(): List<WatchHistoryItem> = store.load()

    fun listByType(mediaType: MediaType): List<WatchHistoryItem> = store.listByType(mediaType)

    fun listResultsByType(mediaType: MediaType): List<SearchResult> =
        store.listByType(mediaType).map { it.toSearchResult() }

    fun recordPlayback(
        item: PlaybackItem,
        seasonNumber: Int?,
        episodeNumber: Int?
    ) {
        store.record(item.toWatchHistoryItem(seasonNumber, episodeNumber))
    }
}

fun PlaybackItem.toWatchHistoryItem(
    seasonNumber: Int?,
    episodeNumber: Int?
): WatchHistoryItem {
    val isEpisode = mediaRef.mediaType == MediaType.SHOW && seasonNumber != null && episodeNumber != null
    return WatchHistoryItem(
        mediaType = mediaRef.mediaType,
        ids = mediaRef.ids,
        title = mediaRef.title,
        year = mediaRef.year,
        artworkUrl = artworkUrl,
        subtitle = subtitle,
        seasonNumber = if (isEpisode) seasonNumber else null,
        episodeNumber = if (isEpisode) episodeNumber else null,
        episodeTitle = if (isEpisode) subtitle else null
    )
}

fun WatchHistoryItem.toSearchResult(): SearchResult {
    val effectiveSubtitle = when {
        mediaType == MediaType.SHOW && seasonNumber != null && episodeNumber != null && !episodeTitle.isNullOrBlank() -> {
            "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')} • $episodeTitle"
        }
        mediaType == MediaType.SHOW && seasonNumber != null && episodeNumber != null -> {
            "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
        }
        else -> subtitle
    }
    return SearchResult(
        mediaRef = MediaRef(
            mediaType = mediaType,
            ids = ids,
            title = title,
            year = year
        ),
        subtitle = effectiveSubtitle,
        posterUrl = artworkUrl,
        backdropUrl = artworkUrl,
        badges = listOf("Watched")
    )
}
