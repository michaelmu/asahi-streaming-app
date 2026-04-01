package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceProvider

class FakeSourceProvider : SourceProvider {
    override val id: String = "fake"
    override val displayName: String = "Fake Provider"
    override val kind: ProviderKind = ProviderKind.SCRAPER

    override suspend fun search(request: SourceSearchRequest): List<RawProviderSource> {
        if (request.mediaRef.title.isBlank()) return emptyList()
        return listOf(
            RawProviderSource(
                providerId = id,
                title = "${request.mediaRef.title} 1080p WEB",
                url = "https://example.com/stream/${request.mediaRef.title}",
                infoHash = null,
                sizeBytes = 2_000_000_000,
                seeders = 42,
                extra = mapOf("quality_hint" to "1080p")
            )
        )
    }
}
