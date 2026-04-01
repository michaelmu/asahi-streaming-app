package ai.shieldtv.app.ui

import ai.shieldtv.app.feature.DetailsFeatureFactory
import ai.shieldtv.app.feature.SearchFeatureFactory
import kotlinx.coroutines.runBlocking

class DebugAppPreviewBuilder {
    private val sourcePreviewBuilder = SourcePreviewBuilder()
    private val playbackPreviewBuilder = PlaybackPreviewBuilder()

    fun build(): String = runBlocking {
        val searchViewModel = SearchFeatureFactory.createViewModel()
        val detailsViewModel = DetailsFeatureFactory.createViewModel()

        val searchState = searchViewModel.search("asahi")
        val firstResult = searchState.results.firstOrNull()
        val detailsState = firstResult?.let { detailsViewModel.load(it.mediaRef) }
        val sourcePreview = sourcePreviewBuilder.build(searchViewModel)
        val playbackPreview = playbackPreviewBuilder.build(searchViewModel)

        buildString {
            appendLine("Preview Query: asahi")
            appendLine(MetadataPreviewFormatter.describe(searchState.results).trim())
            appendLine()
            appendLine("Selected Details:")
            appendLine(detailsState?.item?.mediaRef?.title ?: "none")
            appendLine(detailsState?.item?.overview ?: "none")
            appendLine()
            appendLine(sourcePreview)
            appendLine()
            append(playbackPreview)
        }
    }
}
