package ai.shieldtv.app.integration.scrapers.provider.real

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.integration.scrapers.provider.ProviderQuerySanitizer
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderQueryBuilder
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest

class RealProviderQueryBuilder : ProviderQueryBuilder {
    override fun build(request: SourceSearchRequest): ProviderRequest {
        val query = ProviderQuerySanitizer.toQuery(request)
        return ProviderRequest(
            query = query,
            params = buildMap {
                put("query", query)
                request.mediaRef.year?.let { put("year", it.toString()) }
                request.seasonNumber?.let { put("season", it.toString()) }
                request.episodeNumber?.let { put("episode", it.toString()) }
            }
        )
    }
}
