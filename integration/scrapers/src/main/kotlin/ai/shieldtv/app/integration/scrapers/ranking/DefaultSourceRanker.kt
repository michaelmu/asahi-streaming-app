package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.source.ranking.SourceFilterEngine
import ai.shieldtv.app.domain.source.ranking.SourceRanker

class DefaultSourceRanker(
    private val sourceFilterEngine: SourceFilterEngine = DefaultSourceFilterEngine()
) : SourceRanker {
    override fun rank(sources: List<SourceResult>, filters: SourceFilters): List<SourceResult> {
        return sourceFilterEngine.apply(sources, filters)
            .sortedByDescending { rankScore(it) }
    }

    private fun rankScore(source: SourceResult): Long {
        val qualityScore = when (source.quality) {
            Quality.UHD_4K -> 4000L
            Quality.FHD_1080P -> 3000L
            Quality.HD_720P -> 2000L
            Quality.SD -> 1000L
            Quality.SCR -> 200L
            Quality.CAM -> 100L
            Quality.TELE -> 50L
            Quality.UNKNOWN -> 10L
        }
        val cacheScore = when (source.cacheStatus) {
            CacheStatus.CACHED -> 10000L
            CacheStatus.DIRECT -> 8000L
            CacheStatus.UNCHECKED -> 100L
            CacheStatus.UNCACHED -> 0L
        }
        val seederScore = (source.rawMetadata["seeders"]?.toIntOrNull() ?: 0).coerceAtMost(500).toLong()
        val sizeScore = ((source.sizeBytes ?: 0L) / (1024L * 1024L * 1024L)).coerceAtMost(100L)
        return cacheScore + qualityScore + seederScore + sizeScore
    }
}
