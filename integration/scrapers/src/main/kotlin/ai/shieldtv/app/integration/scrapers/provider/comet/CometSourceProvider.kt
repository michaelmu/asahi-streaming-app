package ai.shieldtv.app.integration.scrapers.provider.comet

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.ProviderCapabilities
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceProvider
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenProvider
import ai.shieldtv.app.integration.scrapers.provider.template.TemplateBackedSourceProviderAdapter

class CometSourceProvider(
    tokenProvider: RealDebridTokenProvider
) : SourceProvider {
    private val adapter = TemplateBackedSourceProviderAdapter(
        queryBuilder = CometQueryBuilder(rdTokenProvider = { tokenProvider.getAccessToken() }),
        transport = CometTransport(),
        parser = CometParser()
    )

    override val id: String = "comet"
    override val displayName: String = "Comet"
    override val kind: ProviderKind = ProviderKind.SCRAPER
    override val capabilities: ProviderCapabilities = ProviderCapabilities(
        supportsMovies = true,
        supportsEpisodes = true,
        supportsSeasonPacks = false,
        supportsSeriesPacks = false,
        requiresRealDebrid = true,
        returnsMagnets = true,
        returnsResolvedLinks = false,
        productionReady = true
    )

    override suspend fun search(request: SourceSearchRequest): List<RawProviderSource> {
        return adapter.fetch("", request)
    }
}
