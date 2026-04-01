package ai.shieldtv.app.integration.metadata.tmdb.repository

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.domain.repository.MetadataRepository
import ai.shieldtv.app.integration.metadata.tmdb.api.TmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbDetailsMapper
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbSeasonMapper
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbSearchMapper

class TmdbMetadataRepository(
    private val tmdbApi: TmdbApi,
    private val tmdbSearchMapper: TmdbSearchMapper,
    private val tmdbDetailsMapper: TmdbDetailsMapper,
    private val tmdbSeasonMapper: TmdbSeasonMapper,
    private val fallbackTmdbApi: TmdbApi
) : MetadataRepository {
    override suspend fun search(query: String): List<SearchResult> {
        val response = runCatching { tmdbApi.searchMulti(query) }
            .getOrElse { fallbackTmdbApi.searchMulti(query) }
        return tmdbSearchMapper.fromQueryEcho(response)
    }

    override suspend fun getTitleDetails(mediaRef: MediaRef): TitleDetails {
        val tmdbId = mediaRef.ids.tmdbId ?: return tmdbDetailsMapper.fromMediaRef(mediaRef)
        val mediaType = when (mediaRef.mediaType) {
            MediaType.MOVIE -> "movie"
            MediaType.SHOW -> "tv"
            else -> return tmdbDetailsMapper.fromMediaRef(mediaRef)
        }
        val response = runCatching { tmdbApi.getDetails(tmdbId, mediaType) }
            .getOrElse { fallbackTmdbApi.getDetails(tmdbId, mediaType) }
        val baseDetails = tmdbDetailsMapper.fromJson(response, mediaRef)

        if (mediaRef.mediaType != MediaType.SHOW) {
            return baseDetails
        }

        val seasonCount = baseDetails.seasonCount ?: return baseDetails
        val seasonEpisodes = buildMap {
            for (season in 1..seasonCount.coerceAtMost(8)) {
                val seasonJson = runCatching { tmdbApi.getSeasonDetails(tmdbId, season) }
                    .getOrElse { fallbackTmdbApi.getSeasonDetails(tmdbId, season) }
                val episodes = tmdbSeasonMapper.fromJson(seasonJson)
                if (episodes.isNotEmpty()) {
                    put(season, episodes)
                }
            }
        }

        return baseDetails.copy(episodesBySeason = seasonEpisodes)
    }
}
