package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.source.ranking.SourceFilterEngine
import ai.shieldtv.app.domain.source.ranking.SourceRanker

class DefaultSourceRanker(
    private val sourceFilterEngine: SourceFilterEngine = DefaultSourceFilterEngine()
) : SourceRanker {
    override fun rank(sources: List<SourceResult>, filters: SourceFilters): List<SourceResult> {
        return sourceFilterEngine.apply(sources, filters)
            .sortedByDescending { it.score ?: 0.0 }
    }
}
