package ai.shieldtv.app.feature.sources.presentation

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.feature.sources.ui.SourcesUiState

open class SourcesViewModel(
    private val sourcesPresenter: SourcesPresenter
) {
    open suspend fun load(
        request: SourceSearchRequest,
        enabledProviderIds: Set<String> = emptySet(),
        onProgress: ((SourceFetchProgress) -> Unit)? = null
    ): SourcesUiState {
        return sourcesPresenter.load(request, enabledProviderIds, onProgress)
    }
}
