package ai.shieldtv.app.integration.metadata.tmdb.api

interface TmdbApi {
    suspend fun searchMulti(query: String): String
}
