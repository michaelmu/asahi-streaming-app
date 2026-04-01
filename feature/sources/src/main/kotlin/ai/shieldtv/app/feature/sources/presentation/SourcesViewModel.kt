package ai.shieldtv.app.feature.sources.presentation

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.feature.sources.ui.SourcesUiState

class SourcesViewModel(
    private val sourcesPresenter: SourcesPresenter
) {
    suspend fun load(request: SourceSearchRequest): SourcesUiState {
        return sourcesPresenter.load(request)
    }
}
