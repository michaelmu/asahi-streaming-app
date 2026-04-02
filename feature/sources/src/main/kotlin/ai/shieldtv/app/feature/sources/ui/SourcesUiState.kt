package ai.shieldtv.app.feature.sources.ui

import ai.shieldtv.app.core.model.source.SourceResult

data class SourcesUiState(
    val loading: Boolean = false,
    val sources: List<SourceResult> = emptyList(),
    val diagnostics: String? = null,
    val error: String? = null
)
