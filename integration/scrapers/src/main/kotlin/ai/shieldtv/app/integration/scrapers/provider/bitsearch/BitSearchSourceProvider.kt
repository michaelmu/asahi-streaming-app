package ai.shieldtv.app.integration.scrapers.provider.bitsearch

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.ProviderCapabilities
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceProvider
import ai.shieldtv.app.integration.scrapers.provider.template.TemplateBackedSourceProviderAdapter

class BitSearchSourceProvider : SourceProvider {
    private val adapter = TemplateBackedSourceProviderAdapter(
        queryBuilder = BitSearchQueryBuilder(),
        transport = BitSearchTransport(),
        parser = BitSearchParser()
    )

    override val id: String = "bitsearch"
    override val displayName: String = "BitSearch"
    override val kind: ProviderKind = ProviderKind.SCRAPER
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsMovies = true,
        supportsEpisodes = true,
        supportsSeasonPacks = false,
        supportsSeriesPacks = false,
        requiresRealDebrid = false,
        returnsMagnets = true,
        returnsResolvedLinks = false,
        productionReady = true
    )

    override suspend fun search(request: SourceSearchRequest): List<RawProviderSource> {
        return adapter.fetch("", request)
    }
}
