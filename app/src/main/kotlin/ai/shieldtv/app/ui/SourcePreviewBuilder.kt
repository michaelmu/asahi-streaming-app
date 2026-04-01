package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.feature.DetailsFeatureFactory
import ai.shieldtv.app.feature.SourcesFeatureFactory
import ai.shieldtv.app.feature.search.presentation.SearchViewModel

class SourcePreviewBuilder {
    suspend fun build(searchViewModel: SearchViewModel, query: String): String {
        val searchState = searchViewModel.search(query)
        val firstResult = searchState.results.firstOrNull() ?: return "No source preview available"

        val detailsViewModel = DetailsFeatureFactory.createViewModel()
        val detailsState = detailsViewModel.load(firstResult.mediaRef)
        val sourceMediaRef = detailsState.item?.mediaRef ?: firstResult.mediaRef

        val sourcesViewModel = SourcesFeatureFactory.createViewModel()
        val sourcesState = sourcesViewModel.load(
            SourceSearchRequest(mediaRef = sourceMediaRef)
        )

        val byProvider = sourcesState.sources.groupingBy { it.providerId }.eachCount()
        val torrentioCount = sourcesState.sources.count { (it.rawMetadata["transport"] ?: "").contains("torrentio") || it.providerId == "torrentio" }

        return buildString {
            appendLine("Sources Preview:")
            appendLine("Source media TMDb: ${sourceMediaRef.ids.tmdbId ?: "none"}")
            appendLine("Source media IMDb: ${sourceMediaRef.ids.imdbId ?: "none"}")
            appendLine("Total sources: ${sourcesState.sources.size}")
            appendLine("Torrentio-like sources: $torrentioCount")
            appendLine("By provider: ${if (byProvider.isEmpty()) "none" else byProvider.entries.joinToString { "${it.key}=${it.value}" }}")
            sourcesState.sources.forEachIndexed { index, source ->
                val transport = source.rawMetadata["transport"] ?: "in-memory"
                val query = source.rawMetadata["query"] ?: "n/a"
                appendLine(
                    "${index + 1}. ${source.displayName} [${source.providerDisplayName}] ${source.quality} " +
                        "provider=${source.providerId} cache=${source.cacheStatus} transport=$transport query=$query"
                )
            }
        }
    }
}
