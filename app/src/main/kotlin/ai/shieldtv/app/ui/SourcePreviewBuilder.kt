package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.feature.SourcesFeatureFactory
import ai.shieldtv.app.feature.search.presentation.SearchViewModel

class SourcePreviewBuilder {
    suspend fun build(searchViewModel: SearchViewModel): String {
        val searchState = searchViewModel.search("asahi")
        val firstResult = searchState.results.firstOrNull() ?: return "No source preview available"

        val sourcesViewModel = SourcesFeatureFactory.createViewModel()
        val sourcesState = sourcesViewModel.load(
            SourceSearchRequest(mediaRef = firstResult.mediaRef)
        )

        return buildString {
            appendLine("Sources Preview:")
            appendLine("Total sources: ${sourcesState.sources.size}")
            sourcesState.sources.forEachIndexed { index, source ->
                val transport = source.rawMetadata["transport"] ?: "in-memory"
                val query = source.rawMetadata["query"] ?: "n/a"
                appendLine("${index + 1}. ${source.displayName} [${source.providerDisplayName}] ${source.quality} transport=$transport query=$query")
            }
        }
    }
}
