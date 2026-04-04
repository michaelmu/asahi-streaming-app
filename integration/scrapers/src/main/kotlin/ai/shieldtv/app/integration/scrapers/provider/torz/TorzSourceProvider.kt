package ai.shieldtv.app.integration.scrapers.provider.torz

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.ProviderCapabilities
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceProvider
import ai.shieldtv.app.integration.scrapers.provider.template.TemplateBackedSourceProviderAdapter

class TorzSourceProvider : SourceProvider {
    private val adapter = TemplateBackedSourceProviderAdapter(
        queryBuilder = TorzQueryBuilder(),
        transport = TorzTransport(),
        parser = TorzParser()
    )

    override val id: String = "torz"
    override val displayName: String = "Torz"
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
