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
            Quality.UHD_4K -> if ((source.sizeBytes ?: 0L) <= 22L * 1024L * 1024L * 1024L) 4600L else 3000L
            Quality.FHD_1080P -> 4200L
            Quality.HD_720P -> 3200L
            Quality.SD -> 1200L
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
        val sizePenalty = sizePenalty(source)
        val remuxPenalty = if (source.displayName.contains("remux", ignoreCase = true)) {
            if (source.quality == Quality.UHD_4K) 600L else 1200L
        } else 0L
        return cacheScore + qualityScore + seederScore - sizePenalty - remuxPenalty
    }

    private fun sizePenalty(source: SourceResult): Long {
        val sizeGb = (source.sizeBytes ?: 0L) / (1024L * 1024L * 1024L)
        return when (source.quality) {
            Quality.UHD_4K -> when {
                sizeGb > 40L -> 1200L
                sizeGb > 25L -> 700L
                else -> 0L
            }
            Quality.FHD_1080P -> when {
                sizeGb > 20L -> 1000L
                sizeGb > 12L -> 500L
                else -> 0L
            }
            Quality.HD_720P -> when {
                sizeGb > 10L -> 700L
                sizeGb > 6L -> 300L
                else -> 0L
            }
            else -> 0L
        }
    }
}
