package ai.shieldtv.app.integration.scrapers.ranking

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult

internal object DefaultSourceScoreRules {
    fun create(): List<SourceScoreRule> = listOf(
        CachePreferenceRule(),
        QualityPreferenceRule(),
        SeederScoreRule(),
        ProviderPreferenceRule(),
        SizePenaltyRule(),
        RemuxPenaltyRule(),
        CamPenaltyRule(),
        TinyFilePenaltyRule()
    )
}

private class CachePreferenceRule : SourceScoreRule {
    override fun evaluate(source: SourceResult): SourceScoreContribution {
        val value = when (source.cacheStatus) {
            CacheStatus.CACHED -> 10000L
            CacheStatus.DIRECT -> 8000L
            CacheStatus.UNCHECKED -> 100L
            CacheStatus.UNCACHED -> 0L
        }
        return SourceScoreContribution("cache", value, "cacheStatus=${source.cacheStatus}")
    }
}

private class QualityPreferenceRule : SourceScoreRule {
    override fun evaluate(source: SourceResult): SourceScoreContribution {
        val value = when (source.quality) {
            Quality.UHD_4K -> if ((source.sizeBytes ?: 0L) <= 22L * 1024L * 1024L * 1024L) 4600L else 3000L
            Quality.FHD_1080P -> 4200L
            Quality.HD_720P -> 3200L
            Quality.SD -> 1200L
            Quality.SCR -> 200L
            Quality.CAM -> 100L
            Quality.TELE -> 50L
            Quality.UNKNOWN -> 10L
        }
        return SourceScoreContribution("quality", value, "quality=${source.quality}")
    }
}

private class SeederScoreRule : SourceScoreRule {
    override fun evaluate(source: SourceResult): SourceScoreContribution? {
        val seeders = (source.rawMetadata["seeders"]?.toIntOrNull() ?: 0).coerceAtMost(500)
        if (seeders == 0) return null
        return SourceScoreContribution("seeders", seeders.toLong(), "seeders=$seeders")
    }
}

private class ProviderPreferenceRule : SourceScoreRule {
    override fun evaluate(source: SourceResult): SourceScoreContribution {
        val primary = source.providerIds.firstOrNull() ?: source.providerId
        val value = when (primary.lowercase()) {
            "torrentio" -> 900L
            "comet" -> 850L
            "zilean" -> 800L
            "torz" -> 780L
            "bitmagnet" -> 740L
            "bitsearch" -> 620L
            "knaben" -> 600L
            else -> 300L
        } + ((source.providerIds.size - 1).coerceAtLeast(0) * 40L)
        return SourceScoreContribution("provider", value, "primary=$primary providers=${source.providerIds.size}")
    }
}

private class SizePenaltyRule : SourceScoreRule {
    override fun evaluate(source: SourceResult): SourceScoreContribution? {
        val sizeGb = (source.sizeBytes ?: 0L) / (1024L * 1024L * 1024L)
        val penalty = when (source.quality) {
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
        if (penalty == 0L) return null
        return SourceScoreContribution("sizePenalty", -penalty, "sizeGb=$sizeGb quality=${source.quality}")
    }
}

private class RemuxPenaltyRule : SourceScoreRule {
    override fun evaluate(source: SourceResult): SourceScoreContribution? {
        if (!source.displayName.contains("remux", ignoreCase = true)) return null
        val penalty = if (source.quality == Quality.UHD_4K) 600L else 1200L
        return SourceScoreContribution("remuxPenalty", -penalty, "displayName contains remux")
    }
}

private class CamPenaltyRule : SourceScoreRule {
    override fun evaluate(source: SourceResult): SourceScoreContribution? {
        val penalty = if (source.quality == Quality.CAM || source.quality == Quality.TELE || source.quality == Quality.SCR) 5000L else 0L
        if (penalty == 0L) return null
        return SourceScoreContribution("camPenalty", -penalty, "quality=${source.quality}")
    }
}

private class TinyFilePenaltyRule : SourceScoreRule {
    override fun evaluate(source: SourceResult): SourceScoreContribution? {
        val sizeGb = (source.sizeBytes ?: return null).toDouble() / (1024.0 * 1024.0 * 1024.0)
        val penalty = when (source.quality) {
            Quality.UHD_4K -> if (sizeGb < 5.0) 2500L else 0L
            Quality.FHD_1080P -> if (sizeGb < 1.2) 1500L else 0L
            Quality.HD_720P -> if (sizeGb < 0.5) 800L else 0L
            else -> 0L
        }
        if (penalty == 0L) return null
        return SourceScoreContribution("tinyPenalty", -penalty, "sizeGb=$sizeGb quality=${source.quality}")
    }
}
