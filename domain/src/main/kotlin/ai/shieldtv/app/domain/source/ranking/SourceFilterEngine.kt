package ai.shieldtv.app.domain.source.ranking

import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult

interface SourceFilterEngine {
    fun apply(sources: List<SourceResult>, filters: SourceFilters): List<SourceResult>
}
