package ai.shieldtv.app.domain.source.ranking

import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult

interface SourceRanker {
    fun rank(sources: List<SourceResult>, filters: SourceFilters): List<SourceResult>

    fun explain(source: SourceResult): SourceRankingExplanation? = null
}
