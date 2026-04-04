package ai.shieldtv.app.history

import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.playback.PlaybackItem

class WatchHistoryCoordinator(
    private val store: WatchHistoryStore
) {
    fun list(): List<WatchHistoryItem> = store.load()

    fun listByType(mediaType: MediaType): List<WatchHistoryItem> = store.listByType(mediaType)

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
