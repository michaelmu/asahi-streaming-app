package ai.shieldtv.app.feature.search.presentation

import ai.shieldtv.app.feature.search.ui.SearchUiState

class SearchViewModel(
    private val searchPresenter: SearchPresenter
) {
    suspend fun search(query: String): SearchUiState {
        return searchPresenter.search(query)
    }
}
