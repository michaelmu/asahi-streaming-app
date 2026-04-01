package ai.shieldtv.app.integration.scrapers.normalize

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceNormalizer
import ai.shieldtv.app.domain.provider.SourceProvider

class DefaultSourceNormalizer : SourceNormalizer {
    override fun normalize(
        request: SourceSearchRequest,
        provider: SourceProvider,
        raw: RawProviderSource
    ): SourceResult {
        val quality = when (raw.extra["quality_hint"]?.lowercase()) {
            "4k" -> Quality.UHD_4K
            "1080p" -> Quality.FHD_1080P
            "720p" -> Quality.HD_720P
            "sd" -> Quality.SD
            else -> Quality.UNKNOWN
        }

        return SourceResult(
            id = "${provider.id}:${raw.title}:${raw.infoHash ?: raw.url}",
            mediaRef = request.mediaRef,
            providerId = provider.id,
            providerDisplayName = provider.displayName,
            providerKind = provider.kind,
            debridService = DebridService.NONE,
            sourceSite = provider.displayName,
            url = raw.url,
            displayName = raw.title,
            quality = quality,
            cacheStatus = CacheStatus.UNCHECKED,
            infoHash = raw.infoHash,
            sizeBytes = raw.sizeBytes,
            rawMetadata = buildMap {
                putAll(raw.extra)
                raw.seeders?.let { put("seeders", it.toString()) }
            }
        )
    }
}
