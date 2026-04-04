package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.source.ranking.SourceFilterEngine

class DefaultSourceFilterEngine : SourceFilterEngine {
    override fun apply(sources: List<SourceResult>, filters: SourceFilters): List<SourceResult> {
        return sources.filter { source ->
            val qualityAllowed = filters.allowedQualities.isEmpty() || source.quality in filters.allowedQualities
            val cacheAllowed = !filters.requireCachedOnly || source.cacheStatus == CacheStatus.CACHED
            val sizeAllowed = when (source.mediaRef.mediaType) {
                MediaType.MOVIE -> withinLimit(source.sizeBytes, filters.movieMaxSizeGb)
                MediaType.SHOW, MediaType.EPISODE, MediaType.SEASON -> withinLimit(source.sizeBytes, filters.episodeMaxSizeGb)
            }
            qualityAllowed && cacheAllowed && sizeAllowed
        }
    }

    private fun withinLimit(sizeBytes: Long?, maxSizeGb: Int?): Boolean {
        if (maxSizeGb == null || maxSizeGb <= 0 || sizeBytes == null) return true
        val maxBytes = maxSizeGb.toLong() * 1024L * 1024L * 1024L
        return sizeBytes <= maxBytes
    }
}
