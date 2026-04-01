package ai.shieldtv.app.feature

import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.feature.sources.presentation.SourcesPresenter
import ai.shieldtv.app.feature.sources.presentation.SourcesViewModel

object SourcesFeatureFactory {
    fun createViewModel(): SourcesViewModel {
        val presenter = SourcesPresenter(AppContainer.findSourcesUseCase)
        return SourcesViewModel(presenter)
    }
}
