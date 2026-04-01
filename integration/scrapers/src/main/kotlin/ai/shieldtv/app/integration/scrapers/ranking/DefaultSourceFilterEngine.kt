package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.source.ranking.SourceFilterEngine

class DefaultSourceFilterEngine : SourceFilterEngine {
    override fun apply(sources: List<SourceResult>, filters: SourceFilters): List<SourceResult> {
        return sources.filter { source ->
            val qualityAllowed = filters.allowedQualities.isEmpty() || source.quality in filters.allowedQualities
            val cacheAllowed = !filters.requireCachedOnly || source.cacheStatus == CacheStatus.CACHED
            qualityAllowed && cacheAllowed
        }
    }
}
