package ai.shieldtv.app

import ai.shieldtv.app.continuewatching.PersistedContinueWatchingItem
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.navigation.AppDestination
import ai.shieldtv.app.navigation.AppNavigator

class AppCoordinator(
    private val navigator: AppNavigator = AppNavigator()
) {
    private var state: AppState = AppState()

    fun currentState(): AppState = state

    fun restoreState(restoredState: AppState) {
        state = restoredState
        navigator.goTo(restoredState.destination)
    }

    fun openHome() {
        state = state.copy(destination = AppDestination.HOME)
        navigator.goTo(AppDestination.HOME)
    }

    fun openSearch(mode: SearchMode) {
        val preserveQuery = state.destination == AppDestination.RESULTS &&
            state.favoritesBrowseMode == null &&
            state.historyBrowseMode == null
        state = state.copy(
            destination = AppDestination.SEARCH,
            searchMode = mode,
            query = if (preserveQuery) state.query else "",
            selectedMedia = null,
            selectedDetails = null,
            selectedSeasonNumber = null,
            selectedEpisodeNumber = null,
            selectedSource = null,
            selectedSources = emptyList(),
            favoritesBrowseMode = null,
            historyBrowseMode = null
        )
        navigator.goTo(AppDestination.SEARCH)
    }

    fun showResults(query: String, results: List<SearchResult>) {
        val updatedRecentQueries = (listOf(query) + state.recentQueries.filterNot { it.equals(query, ignoreCase = true) })
            .take(6)
        state = state.copy(
            destination = AppDestination.RESULTS,
            query = query,
            searchResults = results,
            selectedMedia = null,
            selectedDetails = null,
            selectedSeasonNumber = null,
            selectedEpisodeNumber = null,
            selectedSource = null,
            selectedSources = emptyList(),
            recentQueries = updatedRecentQueries,
            favoritesBrowseMode = null,
            historyBrowseMode = null
        )
        navigator.goTo(AppDestination.RESULTS)
    }

    fun showDetails(mediaRef: MediaRef, details: TitleDetails) {
        state = state.copy(
            destination = AppDestination.DETAILS,
            selectedMedia = mediaRef,
            selectedDetails = details,
            selectedSeasonNumber = if (mediaRef.mediaType.name == "SHOW") 1 else null,
            selectedEpisodeNumber = if (mediaRef.mediaType.name == "SHOW") 1 else null,
            selectedSource = null,
            selectedSources = emptyList()
        )
        navigator.goTo(AppDestination.DETAILS)
    }

    fun showEpisodes(details: TitleDetails, seasonNumber: Int?, episodeNumber: Int?) {
        state = state.copy(
            destination = AppDestination.EPISODES,
            selectedDetails = details,
            selectedSeasonNumber = seasonNumber,
            selectedEpisodeNumber = episodeNumber,
            selectedSource = null,
            selectedSources = emptyList()
        )
        navigator.goTo(AppDestination.EPISODES)
    }

    fun showSources(
        mediaRef: MediaRef,
        details: TitleDetails?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        sources: List<SourceResult>
    ) {
        state = state.copy(
            destination = AppDestination.SOURCES,
            selectedMedia = mediaRef,
            selectedDetails = details,
            selectedSeasonNumber = seasonNumber,
            selectedEpisodeNumber = episodeNumber,
            selectedSource = state.selectedSource?.takeIf { current ->
                sources.any { it.id == current.id || it.url == current.url }
            } ?: ai.shieldtv.app.di.AppContainer.playbackMemoryStore.find(mediaRef, seasonNumber, episodeNumber)?.let { memory ->
                sources.firstOrNull { source ->
                    (memory.sourceId != null && source.id == memory.sourceId) ||
                        (memory.sourceUrl != null && source.url == memory.sourceUrl)
                }
            },
            selectedSources = sources
        )
        navigator.goTo(AppDestination.SOURCES)
    }

    fun showPlayer(sourceResult: SourceResult) {
        state = state.copy(
            destination = AppDestination.PLAYER,
            selectedSource = sourceResult
        )
        navigator.goTo(AppDestination.PLAYER)
    }

    fun showFavorites(mode: SearchMode, results: List<SearchResult>) {
        state = state.copy(
            destination = AppDestination.RESULTS,
            searchMode = mode,
            query = "Favorites",
            searchResults = results,
            selectedMedia = null,
            selectedDetails = null,
            selectedSeasonNumber = null,
            selectedEpisodeNumber = null,
            selectedSource = null,
            selectedSources = emptyList(),
            favoritesBrowseMode = mode,
            historyBrowseMode = null
        )
        navigator.goTo(AppDestination.RESULTS)
    }

    fun showHistory(mode: SearchMode, results: List<SearchResult>) {
        state = state.copy(
            destination = AppDestination.RESULTS,
            searchMode = mode,
            query = "Watch History",
            searchResults = results,
            selectedMedia = null,
            selectedDetails = null,
            selectedSeasonNumber = null,
            selectedEpisodeNumber = null,
            selectedSource = null,
            selectedSources = emptyList(),
            favoritesBrowseMode = null,
            historyBrowseMode = mode
        )
        navigator.goTo(AppDestination.RESULTS)
    }

    fun openSettings() {
        state = state.copy(destination = AppDestination.SETTINGS)
        navigator.goTo(AppDestination.SETTINGS)
    }

    fun recordContinueWatching(
        mediaRef: MediaRef,
        artworkUrl: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        progressPercent: Int
    ): ContinueWatchingItem {
        val subtitle = if (mediaRef.mediaType == MediaType.SHOW && seasonNumber != null && episodeNumber != null) {
            "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
        } else {
            mediaRef.year?.toString() ?: mediaRef.mediaType.name.lowercase().replaceFirstChar { it.uppercase() }
        }
        val item = ContinueWatchingItem(
            mediaTitle = mediaRef.title,
            subtitle = subtitle,
            artworkUrl = artworkUrl,
            queryHint = mediaRef.title,
            progressPercent = progressPercent.coerceIn(0, 100),
            mediaRef = mediaRef
        )
        state = state.copy(
            continueWatching = (listOf(item) + state.continueWatching.filterNot { it.mediaTitle == item.mediaTitle && it.subtitle == item.subtitle })
                .take(6)
        )
        return item
    }

    fun hydrateContinueWatching(items: List<ContinueWatchingItem>) {
        state = state.copy(continueWatching = items.take(6))
    }

    fun persistedContinueWatchingItem(item: ContinueWatchingItem): PersistedContinueWatchingItem {
        return PersistedContinueWatchingItem(
            mediaTitle = item.mediaTitle,
            subtitle = item.subtitle,
            artworkUrl = item.artworkUrl,
            queryHint = item.queryHint,
            progressPercent = item.progressPercent,
            mediaType = item.mediaRef?.mediaType,
            tmdbId = item.mediaRef?.ids?.tmdbId,
            imdbId = item.mediaRef?.ids?.imdbId,
            tvdbId = item.mediaRef?.ids?.tvdbId,
            year = item.mediaRef?.year
        )
    }
}
