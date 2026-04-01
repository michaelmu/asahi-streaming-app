package ai.shieldtv.app.integration.metadata.tmdb.api

class FakeTmdbApi : TmdbApi {
    override suspend fun searchMulti(query: String): String {
        return query
    }

    override suspend fun getDetails(tmdbId: String, mediaType: String): String {
        return "{\"id\":\"$tmdbId\",\"media_type\":\"$mediaType\"}"
    }
}
