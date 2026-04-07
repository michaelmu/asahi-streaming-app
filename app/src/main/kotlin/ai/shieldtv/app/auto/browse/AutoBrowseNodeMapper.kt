package ai.shieldtv.app.auto.browse

import ai.shieldtv.app.auto.model.AutoActionHint
import ai.shieldtv.app.auto.model.AutoBrowseNode
import ai.shieldtv.app.continuewatching.PersistedContinueWatchingItem
import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.favorites.FavoriteItem
import ai.shieldtv.app.history.WatchHistoryItem

object AutoBrowseNodeMapper {
    fun collection(
        id: AutoMediaId.Collection,
        title: String,
        subtitle: String? = null,
        artworkUrl: String? = null,
        actionHint: AutoActionHint = AutoActionHint.OPEN_COLLECTION
    ): AutoBrowseNode {
        return AutoBrowseNode(
            id = id.rawValue,
            title = title,
            subtitle = subtitle,
            browsable = true,
            playable = false,
            artworkUrl = artworkUrl,
            actionHint = actionHint
        )
    }

    fun search(title: String = "Search", subtitle: String? = "Find movies and shows"): AutoBrowseNode {
        val id = AutoMediaId.Search()
        return AutoBrowseNode(
            id = id.rawValue,
            title = title,
            subtitle = subtitle,
            browsable = false,
            playable = false,
            actionHint = AutoActionHint.SEARCH
        )
    }

    fun favorite(item: FavoriteItem): AutoBrowseNode {
        val mediaRef = MediaRef(
            mediaType = item.mediaType,
            ids = item.ids,
            title = item.title,
            year = item.year
        )
        val action = if (item.mediaType == MediaType.MOVIE) {
            AutoActionHint.PLAY_MOVIE
        } else {
            AutoActionHint.PLAY_SHOW_DEFAULT
        }
        return mediaItem(
            mediaRef = mediaRef,
            actionHint = action,
            subtitle = item.subtitle ?: item.year?.toString(),
            artworkUrl = item.artworkUrl
        )
    }

    fun recent(item: WatchHistoryItem): AutoBrowseNode {
        val mediaRef = MediaRef(
            mediaType = item.mediaType,
            ids = item.ids,
            title = item.title,
            year = item.year
        )
        val subtitle = when {
            item.mediaType == MediaType.SHOW && item.seasonNumber != null && item.episodeNumber != null -> {
                val code = "S${item.seasonNumber.toString().padStart(2, '0')}E${item.episodeNumber.toString().padStart(2, '0')}"
                listOfNotNull(code, item.episodeTitle).joinToString(" • ")
            }
            else -> item.subtitle ?: item.year?.toString()
        }
        val action = if (item.mediaType == MediaType.MOVIE) {
            AutoActionHint.RESUME
        } else {
            AutoActionHint.PLAY_EPISODE
        }
        return mediaItem(
            mediaRef = mediaRef,
            actionHint = action,
            subtitle = subtitle,
            artworkUrl = item.artworkUrl,
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber
        )
    }

    fun continueWatching(item: PersistedContinueWatchingItem): AutoBrowseNode {
        val mediaRef = item.mediaType?.let {
            MediaRef(
                mediaType = it,
                ids = MediaIds(
                    tmdbId = item.tmdbId,
                    imdbId = item.imdbId,
                    tvdbId = item.tvdbId
                ),
                title = item.mediaTitle,
                year = item.year
            )
        }
        val action = when (mediaRef?.mediaType) {
            MediaType.MOVIE -> AutoActionHint.RESUME
            MediaType.SHOW -> AutoActionHint.PLAY_SHOW_DEFAULT
            else -> AutoActionHint.RESUME
        }
        val id = mediaRef?.let { AutoMediaId.Item(action = action, mediaRef = it).rawValue }
            ?: AutoMediaId.Collection("continue-watching").rawValue + ":placeholder:${item.stableKey().hashCode()}"
        return AutoBrowseNode(
            id = id,
            title = item.mediaTitle,
            subtitle = item.subtitle.ifBlank { "Resume playback" },
            browsable = false,
            playable = mediaRef != null,
            mediaRef = mediaRef,
            artworkUrl = item.artworkUrl,
            actionHint = action
        )
    }

    private fun mediaItem(
        mediaRef: MediaRef,
        actionHint: AutoActionHint,
        subtitle: String?,
        artworkUrl: String?,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): AutoBrowseNode {
        val id = AutoMediaId.Item(
            action = actionHint,
            mediaRef = mediaRef,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
        return AutoBrowseNode(
            id = id.rawValue,
            title = mediaRef.title,
            subtitle = subtitle,
            browsable = false,
            playable = true,
            mediaRef = mediaRef,
            artworkUrl = artworkUrl,
            actionHint = actionHint
        )
    }
}
