package ai.shieldtv.app.integration.metadata.tmdb.api

interface TmdbApi {
    suspend fun searchMulti(query: String): String
    suspend fun getDetails(tmdbId: String, mediaType: String): String
    suspend fun getSeasonDetails(tmdbId: String, seasonNumber: Int): String
}
