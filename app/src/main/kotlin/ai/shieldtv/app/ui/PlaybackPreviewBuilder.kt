package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.feature.DetailsFeatureFactory
import ai.shieldtv.app.feature.PlayerFeatureFactory
import ai.shieldtv.app.feature.SourcesFeatureFactory
import ai.shieldtv.app.feature.search.presentation.SearchViewModel

class PlaybackPreviewBuilder {
    suspend fun build(searchViewModel: SearchViewModel, query: String): String {
        val searchState = searchViewModel.search(query)
        val firstResult = searchState.results.firstOrNull() ?: return "No playback preview available"

        val detailsViewModel = DetailsFeatureFactory.createViewModel()
        val detailsState = detailsViewModel.load(firstResult.mediaRef)
        val sourceMediaRef = detailsState.item?.mediaRef ?: firstResult.mediaRef

        val sourcesViewModel = SourcesFeatureFactory.createViewModel()
        val sourcesState = sourcesViewModel.load(
            SourceSearchRequest(mediaRef = sourceMediaRef)
        )
        val topRankedSource = sourcesState.sources.firstOrNull() ?: return "No playback preview available"

        val playerViewModel = PlayerFeatureFactory.createViewModel()
        val playerState = playerViewModel.prepare(topRankedSource)

        return buildString {
            appendLine("Playback Preview:")
            appendLine("Prepared source: ${topRankedSource.displayName}")
            appendLine("Prepared provider: ${topRankedSource.providerId}")
            appendLine("Prepared cache status: ${topRankedSource.cacheStatus}")
            appendLine("Player error: ${playerState.error ?: "none"}")
        }
    }
}
