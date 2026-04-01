package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.feature.PlayerFeatureFactory
import ai.shieldtv.app.feature.SourcesFeatureFactory
import ai.shieldtv.app.feature.search.presentation.SearchViewModel

class PlaybackPreviewBuilder {
    suspend fun build(searchViewModel: SearchViewModel): String {
        val searchState = searchViewModel.search("asahi")
        val firstResult = searchState.results.firstOrNull() ?: return "No playback preview available"

        val sourcesViewModel = SourcesFeatureFactory.createViewModel()
        val sourcesState = sourcesViewModel.load(
            SourceSearchRequest(mediaRef = firstResult.mediaRef)
        )
        val firstSource = sourcesState.sources.firstOrNull() ?: return "No playback preview available"

        val playerViewModel = PlayerFeatureFactory.createViewModel()
        val playerState = playerViewModel.prepare(firstSource)

        return buildString {
            appendLine("Playback Preview:")
            appendLine("Prepared source: ${firstSource.displayName}")
            appendLine("Player error: ${playerState.error ?: "none"}")
        }
    }
}
