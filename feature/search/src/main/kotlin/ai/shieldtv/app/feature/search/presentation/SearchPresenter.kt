package ai.shieldtv.app.feature.search.presentation

import ai.shieldtv.app.domain.usecase.search.SearchTitlesUseCase
import ai.shieldtv.app.feature.search.ui.SearchUiState

class SearchPresenter(
    private val searchTitlesUseCase: SearchTitlesUseCase
) {
    suspend fun search(query: String): SearchUiState {
        return try {
            val results = searchTitlesUseCase(query)
            SearchUiState(query = query, results = results)
        } catch (error: Throwable) {
            SearchUiState(query = query, error = error.message)
        }
    }
}
