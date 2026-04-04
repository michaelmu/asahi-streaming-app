package ai.shieldtv.app.feature.sources.presentation

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.feature.sources.ui.SourcesUiState

class SourcesViewModel(
    private val sourcesPresenter: SourcesPresenter
) {
    suspend fun load(
        request: SourceSearchRequest,
        onProgress: ((SourceFetchProgress) -> Unit)? = null
    ): SourcesUiState {
        return sourcesPresenter.load(request, onProgress)
    }
}
