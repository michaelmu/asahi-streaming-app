package ai.shieldtv.app.history

import ai.shieldtv.app.core.model.media.MediaIds
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

    fun applyWatchedBadges(results: List<SearchResult>): List<SearchResult> {
        val watchedKeys = store.load().mapTo(hashSetOf()) { it.stableKey() }
        return results.map { result ->
            val key = result.toWatchHistoryKey()
            if (key != null && key in watchedKeys && "Watched" !in result.badges) {
                result.copy(badges = result.badges + "Watched")
            } else {
                result
            }
        }
    }

    fun watchedEpisodeKeys(showIds: MediaIds): Set<String> {
        return store.listByType(MediaType.SHOW)
            .filter { it.ids == showIds && it.seasonNumber != null && it.episodeNumber != null }
            .mapTo(linkedSetOf()) { it.episodeStableKey() }
    }

    fun removeByResult(result: SearchResult) {
        store.removeMatching(result.toWatchHistoryRemover())
    }

    fun clearByType(mediaType: MediaType) {
        store.clearByType(mediaType)
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

fun SearchResult.toWatchHistoryKey(): String? {
    return when (mediaRef.mediaType) {
        MediaType.MOVIE -> WatchHistoryItem(
            mediaType = mediaRef.mediaType,
            ids = mediaRef.ids,
            title = mediaRef.title,
            year = mediaRef.year
        ).stableKey()
        else -> null
    }
}

fun episodeWatchKey(showIds: MediaIds, showTitle: String, seasonNumber: Int, episodeNumber: Int): String {
    return WatchHistoryItem(
        mediaType = MediaType.SHOW,
        ids = showIds,
        title = showTitle,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber
    ).episodeStableKey()
}

private fun SearchResult.toWatchHistoryRemover(): WatchHistoryItem {
    val episodeMatch = subtitle?.let { subtitleText ->
        Regex("S(\\d{2})E(\\d{2})").find(subtitleText)
    }
    return WatchHistoryItem(
        mediaType = mediaRef.mediaType,
        ids = mediaRef.ids,
        title = mediaRef.title,
        year = mediaRef.year,
        seasonNumber = episodeMatch?.groupValues?.getOrNull(1)?.toIntOrNull(),
        episodeNumber = episodeMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
    )
}

private fun WatchHistoryItem.episodeStableKey(): String = stableKey()
