package ai.shieldtv.app.feature

import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.feature.search.presentation.SearchPresenter
import ai.shieldtv.app.feature.search.presentation.SearchViewModel

object SearchFeatureFactory {
    fun createViewModel(): SearchViewModel {
        val presenter = SearchPresenter(AppContainer.searchTitlesUseCase)
        return SearchViewModel(presenter)
    }
}
