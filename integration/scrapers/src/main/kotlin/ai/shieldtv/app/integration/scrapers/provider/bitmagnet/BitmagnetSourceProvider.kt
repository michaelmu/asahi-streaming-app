package ai.shieldtv.app.integration.scrapers.provider.bitmagnet

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.ProviderCapabilities
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceProvider
import ai.shieldtv.app.integration.scrapers.provider.template.TemplateBackedSourceProviderAdapter

class BitmagnetSourceProvider : SourceProvider {
    private val adapter = TemplateBackedSourceProviderAdapter(
        queryBuilder = BitmagnetQueryBuilder(),
        transport = BitmagnetTransport(),
        parser = BitmagnetParser()
    )

    override val id: String = "bitmagnet"
    override val displayName: String = "Bitmagnet"
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
