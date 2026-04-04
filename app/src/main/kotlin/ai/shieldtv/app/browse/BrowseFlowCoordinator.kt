package ai.shieldtv.app.browse

import ai.shieldtv.app.AppCoordinator
import ai.shieldtv.app.SearchMode
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.feature.details.presentation.DetailsViewModel
import ai.shieldtv.app.feature.search.presentation.SearchViewModel
import ai.shieldtv.app.history.WatchHistoryCoordinator

sealed interface BrowseSearchResult {
    data class Completed(
        val statusMessage: String,
        val results: List<SearchResult>,
        val errorMessage: String?
    ) : BrowseSearchResult
}

sealed interface BrowseSelectionResult {
    data class Completed(
        val statusMessage: String,
        val errorMessage: String?,
        val openedDetails: Boolean
    ) : BrowseSelectionResult
}

class BrowseFlowCoordinator(
    private val searchViewModel: SearchViewModel,
    private val detailsViewModel: DetailsViewModel,
    private val watchHistoryCoordinator: WatchHistoryCoordinator
) {
    suspend fun runSearch(
        coordinator: AppCoordinator,
        mode: SearchMode,
        rawQuery: String
    ): BrowseSearchResult {
        val query = rawQuery.trim()
        val state = searchViewModel.search(query)
        val filteredResults = state.results.filter { it.mediaRef.mediaType == mode.mediaType }
        val decoratedResults = watchHistoryCoordinator.applyWatchedBadges(filteredResults)
        coordinator.showResults(query = query, results = decoratedResults)
        return BrowseSearchResult.Completed(
            statusMessage = state.error ?: "Found ${filteredResults.size} result(s) for \"$query\".",
            results = decoratedResults,
            errorMessage = state.error?.takeIf { decoratedResults.isEmpty() }
        )
    }

    suspend fun openResult(
        coordinator: AppCoordinator,
        result: SearchResult
    ): BrowseSelectionResult {
        val state = detailsViewModel.load(result.mediaRef)
        val details = state.item
        if (details == null) {
            return BrowseSelectionResult.Completed(
                statusMessage = state.error ?: "No details available.",
                errorMessage = state.error ?: "No details available.",
                openedDetails = false
            )
        }

        coordinator.showDetails(result.mediaRef, details)
        return BrowseSelectionResult.Completed(
            statusMessage = "Loaded details for ${details.mediaRef.title}.",
            errorMessage = null,
            openedDetails = true
        )
    }
}
