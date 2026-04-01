package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.RawProviderSource

class FakeSourceProviderAdapter : SourceProviderAdapter {
    override suspend fun fetch(query: String, request: SourceSearchRequest): List<RawProviderSource> {
        if (query.isBlank()) return emptyList()
        return listOf(
            RawProviderSource(
                providerId = "fake",
                title = "$query 1080p WEB",
                url = "https://example.com/stream/${request.mediaRef.title}",
                infoHash = null,
                sizeBytes = 2_000_000_000,
                seeders = 42,
                extra = mapOf("quality_hint" to "1080p", "query" to query)
            )
        )
    }
}
