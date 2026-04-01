package ai.shieldtv.app.feature.sources.presentation

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.feature.sources.ui.SourcesUiState

class SourcesPresenter(
    private val findSourcesUseCase: FindSourcesUseCase
) {
    suspend fun load(request: SourceSearchRequest): SourcesUiState {
        return try {
            val sources = findSourcesUseCase(request)
            SourcesUiState(sources = sources)
        } catch (error: Throwable) {
            SourcesUiState(error = error.message)
        }
    }
}
