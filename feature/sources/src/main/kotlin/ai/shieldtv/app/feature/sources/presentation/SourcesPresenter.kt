package ai.shieldtv.app.feature.sources.presentation

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.feature.sources.ui.SourcesUiState

class SourcesPresenter(
    private val findSourcesUseCase: FindSourcesUseCase
) {
    suspend fun load(
        request: SourceSearchRequest,
        enabledProviderIds: Set<String> = emptySet(),
        onProgress: ((SourceFetchProgress) -> Unit)? = null
    ): SourcesUiState {
        return try {
            val sources = findSourcesUseCase(request, enabledProviderIds, onProgress)
            SourcesUiState(sources = sources)
        } catch (error: Throwable) {
            SourcesUiState(error = error.message)
        }
    }
}
