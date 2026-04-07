package ai.shieldtv.app.auto.browse

import ai.shieldtv.app.auto.model.AutoBrowseNode
import ai.shieldtv.app.continuewatching.ContinueWatchingStore
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.favorites.FavoritesStore
import ai.shieldtv.app.history.WatchHistoryStore

class DefaultAutoBrowseRepository(
    private val favoritesStore: FavoritesStore,
    private val watchHistoryStore: WatchHistoryStore,
    private val continueWatchingStore: ContinueWatchingStore
) : AutoBrowseRepository {

    override suspend fun root() = listOf(
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection("continue-watching"),
            title = "Continue Watching",
            subtitle = "Resume recent playback"
        ),
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection("favorites"),
            title = "Favorites",
            subtitle = "Quick picks you've saved"
        ),
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection("recent"),
            title = "Recent",
            subtitle = "Previously watched items"
        ),
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection("movies"),
            title = "Movies",
            subtitle = "Browse support deferred to a later phase"
        ),
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection("tv-shows"),
            title = "TV Shows",
            subtitle = "Browse support deferred to a later phase"
        ),
        AutoBrowseNodeMapper.search()
    )

    override suspend fun favorites(mediaType: MediaType?) = favoritesStore.load()
        .asSequence()
        .filter { mediaType == null || it.mediaType == mediaType }
        .map(AutoBrowseNodeMapper::favorite)
        .take(MAX_COLLECTION_ITEMS)
        .toList()

    override suspend fun recent(mediaType: MediaType?) = watchHistoryStore.load()
        .asSequence()
        .filter { mediaType == null || it.mediaType == mediaType }
        .map(AutoBrowseNodeMapper::recent)
        .take(MAX_COLLECTION_ITEMS)
        .toList()

    override suspend fun continueWatching() = continueWatchingStore.load()
        .asSequence()
        .map(AutoBrowseNodeMapper::continueWatching)
        .filter { it.playable && it.mediaRef != null }
        .take(MAX_COLLECTION_ITEMS)
        .toList()

    override suspend fun search(query: String, mediaType: MediaType?): List<AutoBrowseNode> = emptyList()

    private companion object {
        const val MAX_COLLECTION_ITEMS = 12
    }
}
