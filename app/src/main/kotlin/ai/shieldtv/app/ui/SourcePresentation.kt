package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult

object SourcePresentation {
    fun groupTitle(source: SourceResult): String = when (source.cacheStatus) {
        CacheStatus.CACHED -> "Ready to Play"
        CacheStatus.DIRECT -> "Direct Links"
        CacheStatus.UNCACHED, CacheStatus.UNCHECKED -> "Other Options"
    }

    fun sourceLabel(source: SourceResult): String = when {
        source.cacheStatus == CacheStatus.CACHED && source.quality == Quality.UHD_4K -> "TOP PICK"
        source.cacheStatus == CacheStatus.CACHED -> "READY"
        source.cacheStatus == CacheStatus.DIRECT -> "DIRECT"
        else -> "OTHER"
    }

    fun qualityLabel(quality: Quality): String = when (quality) {
        Quality.UHD_4K -> "4K"
        Quality.FHD_1080P -> "1080p"
        Quality.HD_720P -> "720p"
        Quality.SD -> "SD"
        Quality.SCR -> "SCR"
        Quality.CAM -> "CAM"
        Quality.TELE -> "TELE"
        Quality.UNKNOWN -> "Unknown"
    }

    fun cacheLabel(cacheStatus: CacheStatus): String = when (cacheStatus) {
        CacheStatus.CACHED -> "Cached"
        CacheStatus.UNCACHED -> "Uncached"
        CacheStatus.UNCHECKED -> "Unchecked"
        CacheStatus.DIRECT -> "Direct"
    }

    fun providerChip(source: SourceResult): String = buildString {
        val providerLabel = when {
            source.providerDisplayNames.size > 1 -> source.providerDisplayNames.joinToString(" + ")
            else -> source.providerDisplayName.ifBlank { source.providerId }
        }
        append(providerLabel)
        source.sourceSite?.takeIf { it.isNotBlank() }?.let {
            append(" · ")
            append(it)
        }
    }

    fun detailLabel(source: SourceResult): String = listOfNotNull(
        sourceLabel(source),
        qualityLabel(source.quality),
        source.sizeLabel,
        source.seedInfo()
    ).joinToString(" • ")

    fun supportLabel(source: SourceResult): String = listOfNotNull(
        providerChip(source),
        source.rawMetadata["flags"]?.takeIf { it.isNotBlank() }?.replace(",", " • ")
    ).joinToString(" • ")

    private fun SourceResult.seedInfo(): String? = rawMetadata["seeders"] ?: score?.let { null }
}
