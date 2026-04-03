package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.ProviderCapabilities
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceProvider

class FakeSourceProvider(
    private val adapter: SourceProviderAdapter = FakeSourceProviderAdapter()
) : SourceProvider {
    override val id: String = "fake"
    override val displayName: String = "Fake Provider"
    override val kind: ProviderKind = ProviderKind.SCRAPER
    override val capabilities: ProviderCapabilities = ProviderCapabilities(productionReady = false)

    override suspend fun search(request: SourceSearchRequest): List<RawProviderSource> {
        val query = ProviderQuerySanitizer.toQuery(request)
        return adapter.fetch(query, request)
    }
}
