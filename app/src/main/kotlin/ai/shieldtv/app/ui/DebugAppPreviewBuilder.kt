package ai.shieldtv.app.ui

import ai.shieldtv.app.feature.DetailsFeatureFactory
import ai.shieldtv.app.feature.SearchFeatureFactory
import kotlinx.coroutines.runBlocking

class DebugAppPreviewBuilder {
    private val sourcePreviewBuilder = SourcePreviewBuilder()
    private val playbackPreviewBuilder = PlaybackPreviewBuilder()
    private val previewQuery = "Dune"

    fun build(): String = runBlocking {
        val searchViewModel = SearchFeatureFactory.createViewModel()
        val detailsViewModel = DetailsFeatureFactory.createViewModel()

        val searchState = searchViewModel.search(previewQuery)
        val firstResult = searchState.results.firstOrNull()
        val detailsState = firstResult?.let { detailsViewModel.load(it.mediaRef) }
        val sourcePreview = sourcePreviewBuilder.build(searchViewModel, previewQuery)
        val playbackPreview = playbackPreviewBuilder.build(searchViewModel, previewQuery)

        buildString {
            appendLine("Preview Query: $previewQuery")
            appendLine(MetadataPreviewFormatter.describe(searchState.results).trim())
            appendLine()
            appendLine(DetailsPreviewFormatter.describe(detailsState?.item).trim())
            appendLine()
            appendLine(sourcePreview)
            appendLine()
            append(playbackPreview)
        }
    }
}
