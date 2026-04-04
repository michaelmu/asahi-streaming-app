package ai.shieldtv.app.sources

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.SourceFetchProgress

data class IncrementalSourceUpdate(
    val sources: List<SourceResult>,
    val progress: List<SourceFetchProgress>,
    val completedProviders: Int,
    val totalProviders: Int
)
