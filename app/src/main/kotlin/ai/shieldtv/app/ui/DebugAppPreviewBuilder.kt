package ai.shieldtv.app.ui

import ai.shieldtv.app.feature.DetailsFeatureFactory
import ai.shieldtv.app.feature.SearchFeatureFactory
import kotlinx.coroutines.runBlocking

class DebugAppPreviewBuilder {
    fun build(): String = runBlocking {
        val searchViewModel = SearchFeatureFactory.createViewModel()
        val detailsViewModel = DetailsFeatureFactory.createViewModel()

        val searchState = searchViewModel.search("asahi")
        val firstResult = searchState.results.firstOrNull()
        val detailsState = firstResult?.let { detailsViewModel.load(it.mediaRef) }

        buildString {
            appendLine("Preview Query: asahi")
            appendLine("Search Results: ${searchState.results.size}")
            searchState.results.forEachIndexed { index, result ->
                appendLine("${index + 1}. ${result.mediaRef.title} (${result.subtitle ?: "no subtitle"})")
            }
            appendLine()
            appendLine("Selected Details:")
            appendLine(detailsState?.item?.mediaRef?.title ?: "none")
            appendLine(detailsState?.item?.overview ?: "none")
        }
    }
}
