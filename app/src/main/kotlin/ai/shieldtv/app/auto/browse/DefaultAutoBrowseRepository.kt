package ai.shieldtv.app.auto.browse

import ai.shieldtv.app.auto.model.AutoBrowseNode
import ai.shieldtv.app.continuewatching.ContinueWatchingStore
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.domain.usecase.search.SearchTitlesUseCase
import ai.shieldtv.app.favorites.FavoritesStore
import ai.shieldtv.app.history.WatchHistoryStore

class DefaultAutoBrowseRepository(
    private val favoritesStore: FavoritesStore,
    private val watchHistoryStore: WatchHistoryStore,
    private val continueWatchingStore: ContinueWatchingStore,
    private val searchTitlesUseCase: SearchTitlesUseCase
) : AutoBrowseRepository {

    override suspend fun root() = listOf(
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection(COLLECTION_CONTINUE_WATCHING),
            title = "Continue Watching",
            subtitle = "Resume recent playback"
        ),
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection(COLLECTION_FAVORITES),
            title = "Favorites",
            subtitle = "Quick picks you've saved"
        ),
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection(COLLECTION_RECENT),
            title = "Recent",
            subtitle = "Previously watched items"
        ),
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection(COLLECTION_MOVIES),
            title = "Movies",
            subtitle = "Movie picks ready to play"
        ),
        AutoBrowseNodeMapper.collection(
            id = AutoMediaId.Collection(COLLECTION_TV_SHOWS),
            title = "TV Shows",
            subtitle = "Show picks with default playback"
        ),
        AutoBrowseNodeMapper.search()
    )

    override suspend fun children(collectionId: String): List<AutoBrowseNode> {
        return when (collectionId) {
            COLLECTION_CONTINUE_WATCHING -> continueWatching().withEmptyState(
                title = "Nothing to resume",
                subtitle = "Start something on TV first"
            )
            COLLECTION_FAVORITES -> favorites(mediaType = null).withEmptyState(
                title = "No favorites yet",
                subtitle = "Save a movie or show on TV first"
            )
            COLLECTION_RECENT -> recent(mediaType = null).withEmptyState(
                title = "Nothing recent",
                subtitle = "Watch something on TV first"
            )
            COLLECTION_MOVIES -> movies().withEmptyState(
                title = "No movies available",
                subtitle = "Search for a movie instead"
            )
            COLLECTION_TV_SHOWS -> tvShows().withEmptyState(
                title = "No shows available",
                subtitle = "Search for a show instead"
            )
            else -> emptyList()
        }
    }

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

    override suspend fun search(query: String, mediaType: MediaType?): List<AutoBrowseNode> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return listOf(
                AutoBrowseNodeMapper.message(
                    key = "search-empty-query",
                    title = "Enter a search",
                    subtitle = "Search for a movie or show"
                )
            )
        }

        val results = searchTitlesUseCase(trimmed)
            .asSequence()
            .filter { mediaType == null || it.mediaRef.mediaType == mediaType }
            .filter { it.mediaRef.mediaType == MediaType.MOVIE || it.mediaRef.mediaType == MediaType.SHOW }
            .map(AutoBrowseNodeMapper::searchResult)
            .take(MAX_SEARCH_ITEMS)
            .toList()

        return results.withEmptyState(
            title = "No results for \"$trimmed\"",
            subtitle = "Try a different title"
        )
    }

    private suspend fun movies(): List<AutoBrowseNode> {
        return dedupePlayableNodes(
            continueWatching()
                .filter { it.mediaRef?.mediaType == MediaType.MOVIE },
            favorites(mediaType = MediaType.MOVIE),
            recent(mediaType = MediaType.MOVIE)
        ).take(MAX_COLLECTION_ITEMS)
    }

    private suspend fun tvShows(): List<AutoBrowseNode> {
        return dedupePlayableNodes(
            continueWatching()
                .filter { it.mediaRef?.mediaType == MediaType.SHOW }
                .map(AutoBrowseNodeMapper::showDefaultAction),
            favorites(mediaType = MediaType.SHOW),
            recent(mediaType = MediaType.SHOW)
                .map(AutoBrowseNodeMapper::showDefaultAction)
        ).take(MAX_COLLECTION_ITEMS)
    }

    private fun dedupePlayableNodes(vararg lists: List<AutoBrowseNode>): List<AutoBrowseNode> {
        val seen = linkedSetOf<String>()
        return lists.asSequence()
            .flatMap { it.asSequence() }
            .filter { node ->
                val key = node.mediaRef?.let { mediaRef ->
                    "${mediaRef.mediaType}:${AutoMediaId.mediaIdentity(mediaRef)}"
                } ?: node.id
                seen.add(key)
            }
            .toList()
    }

    private fun List<AutoBrowseNode>.withEmptyState(title: String, subtitle: String): List<AutoBrowseNode> {
        if (isNotEmpty()) return this
        return listOf(
            AutoBrowseNodeMapper.message(
                key = title.lowercase().replace(' ', '-'),
                title = title,
                subtitle = subtitle
            )
        )
    }

    private companion object {
        const val COLLECTION_CONTINUE_WATCHING = "continue-watching"
        const val COLLECTION_FAVORITES = "favorites"
        const val COLLECTION_RECENT = "recent"
        const val COLLECTION_MOVIES = "movies"
        const val COLLECTION_TV_SHOWS = "tv-shows"
        const val MAX_COLLECTION_ITEMS = 12
        const val MAX_SEARCH_ITEMS = 8
    }
}
