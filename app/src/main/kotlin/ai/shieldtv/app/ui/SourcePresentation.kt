package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult

object SourcePresentation {
    fun groupTitle(source: SourceResult): String = when (source.cacheStatus) {
        CacheStatus.CACHED -> "Cached Picks"
        CacheStatus.DIRECT -> "Direct Links"
        CacheStatus.UNCACHED, CacheStatus.UNCHECKED -> "Fallback Sources"
    }

    fun sourceLabel(source: SourceResult): String = when {
        source.cacheStatus == CacheStatus.CACHED && source.quality == Quality.UHD_4K -> "BEST"
        source.cacheStatus == CacheStatus.CACHED -> "CACHED"
        source.cacheStatus == CacheStatus.DIRECT -> "DIRECT"
        else -> "FALLBACK"
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
}
