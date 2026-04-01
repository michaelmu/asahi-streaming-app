package ai.shieldtv.app.integration.scrapers.normalize

import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
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
            quality = Quality.UNKNOWN,
            cacheStatus = CacheStatus.UNCHECKED,
            infoHash = raw.infoHash,
            sizeBytes = raw.sizeBytes,
            rawMetadata = raw.extra
        )
    }
}
