package ai.shieldtv.app.domain.repository

import ai.shieldtv.app.core.model.source.SourceResult

data class IncrementalSourceResult(
    val sources: List<SourceResult>,
    val completedProviders: Int,
    val totalProviders: Int
)
