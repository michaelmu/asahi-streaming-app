package ai.shieldtv.app.integration.scrapers.provider.zilean

import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderQueryBuilder
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest

class ZileanQueryBuilder : ProviderQueryBuilder {
    override fun build(request: SourceSearchRequest): ProviderRequest {
        val imdbId = request.mediaRef.ids.imdbId ?: return ProviderRequest(query = request.mediaRef.title)
        val path = when (request.mediaRef.mediaType) {
            MediaType.MOVIE -> "/dmm/filtered?ImdbId=$imdbId"
            MediaType.SHOW, MediaType.EPISODE, MediaType.SEASON -> {
                val season = request.seasonNumber ?: 1
                val episode = request.episodeNumber ?: 1
                "/dmm/filtered?ImdbId=$imdbId&Season=$season&Episode=$episode"
            }
        }
        return ProviderRequest(
            query = request.mediaRef.title,
            params = mapOf("path" to path)
        )
    }
}
