package ai.shieldtv.app.feature.details.presentation

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.domain.usecase.details.GetTitleDetailsUseCase
import ai.shieldtv.app.feature.details.ui.DetailsUiState

class DetailsPresenter(
    private val getTitleDetailsUseCase: GetTitleDetailsUseCase
) {
    suspend fun load(mediaRef: MediaRef): DetailsUiState {
        return try {
            val details = getTitleDetailsUseCase(mediaRef)
            DetailsUiState(item = details)
        } catch (error: Throwable) {
            DetailsUiState(error = error.message)
        }
    }
}
