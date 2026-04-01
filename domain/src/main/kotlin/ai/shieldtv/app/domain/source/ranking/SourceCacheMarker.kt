package ai.shieldtv.app.domain.source.ranking

import ai.shieldtv.app.core.model.source.SourceResult

interface SourceCacheMarker {
    suspend fun markCached(sources: List<SourceResult>): List<SourceResult>
}
