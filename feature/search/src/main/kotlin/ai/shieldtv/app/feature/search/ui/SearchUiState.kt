package ai.shieldtv.app.feature.search.ui

import ai.shieldtv.app.core.model.media.SearchResult

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val error: String? = null
)
