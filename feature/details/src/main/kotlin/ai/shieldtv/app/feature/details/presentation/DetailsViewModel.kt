package ai.shieldtv.app.feature.details.presentation

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.feature.details.ui.DetailsUiState

class DetailsViewModel(
    private val detailsPresenter: DetailsPresenter
) {
    suspend fun load(mediaRef: MediaRef): DetailsUiState {
        return detailsPresenter.load(mediaRef)
    }
}
