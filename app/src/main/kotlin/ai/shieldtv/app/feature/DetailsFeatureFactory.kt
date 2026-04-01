package ai.shieldtv.app.feature

import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.feature.details.presentation.DetailsPresenter
import ai.shieldtv.app.feature.details.presentation.DetailsViewModel

object DetailsFeatureFactory {
    fun createViewModel(): DetailsViewModel {
        val presenter = DetailsPresenter(AppContainer.getTitleDetailsUseCase)
        return DetailsViewModel(presenter)
    }
}
