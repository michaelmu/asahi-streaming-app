package ai.shieldtv.app.integration.metadata.tmdb.repository

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.domain.repository.MetadataRepository
import ai.shieldtv.app.integration.metadata.tmdb.api.TmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbDetailsMapper
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbSearchMapper

class TmdbMetadataRepository(
    private val tmdbApi: TmdbApi,
    private val tmdbSearchMapper: TmdbSearchMapper,
    private val tmdbDetailsMapper: TmdbDetailsMapper,
    private val fallbackTmdbApi: TmdbApi
) : MetadataRepository {
    override suspend fun search(query: String): List<SearchResult> {
        val response = runCatching { tmdbApi.searchMulti(query) }
            .getOrElse { fallbackTmdbApi.searchMulti(query) }
        return tmdbSearchMapper.fromQueryEcho(response)
    }

    override suspend fun getTitleDetails(mediaRef: MediaRef): TitleDetails {
        return tmdbDetailsMapper.fromMediaRef(mediaRef)
    }
}
