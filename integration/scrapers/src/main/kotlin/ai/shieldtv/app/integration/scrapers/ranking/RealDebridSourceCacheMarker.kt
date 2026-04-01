package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.DebridCacheRepository
import ai.shieldtv.app.domain.source.ranking.SourceCacheMarker

class RealDebridSourceCacheMarker(
    private val debridCacheRepository: DebridCacheRepository
) : SourceCacheMarker {
    override suspend fun markCached(sources: List<SourceResult>): List<SourceResult> {
        val hashes = sources.mapNotNull { it.infoHash }.distinct()
        if (hashes.isEmpty()) return sources

        val cached = debridCacheRepository.getCachedHashes(hashes)
        return sources.map { source ->
            if (source.infoHash != null && source.infoHash in cached) {
                source.copy(cacheStatus = CacheStatus.CACHED)
            } else {
                source
            }
        }
    }
}
