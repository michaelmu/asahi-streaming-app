package ai.shieldtv.app.integration.scrapers.provider.torrentio

import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderQueryBuilder
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest

class TorrentioQueryBuilder : ProviderQueryBuilder {
    override fun build(request: SourceSearchRequest): ProviderRequest {
        val tmdbLikeId = request.mediaRef.ids.imdbId ?: request.mediaRef.ids.tmdbId ?: request.mediaRef.title
        val path = when (request.mediaRef.mediaType) {
            MediaType.MOVIE -> "/stream/movie/$tmdbLikeId.json"
            MediaType.SHOW, MediaType.EPISODE, MediaType.SEASON -> {
                val season = request.seasonNumber ?: 1
                val episode = request.episodeNumber ?: 1
                "/stream/series/$tmdbLikeId:$season:$episode.json"
            }
        }
        return ProviderRequest(
            query = request.mediaRef.title,
            params = mapOf("path" to path)
        )
    }
}
