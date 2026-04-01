package ai.shieldtv.app.integration.metadata.tmdb.api

class FakeTmdbApi : TmdbApi {
    override suspend fun searchMulti(query: String): String {
        return query
    }
}
