package ai.shieldtv.app.integration.metadata.tmdb.api

class FakeTmdbApi : TmdbApi {
    override suspend fun searchMulti(query: String): String {
        return query
    }

    override suspend fun getDetails(tmdbId: String, mediaType: String): String {
        return "{\"id\":\"$tmdbId\",\"media_type\":\"$mediaType\"}"
    }

    override suspend fun getSeasonDetails(tmdbId: String, seasonNumber: Int): String {
        return buildString {
            append("{\"id\":\"")
            append(tmdbId)
            append("\",\"season_number\":")
            append(seasonNumber)
            append(",\"episodes\":[")
            append((1..10).joinToString(",") { episode ->
                "{\"episode_number\":$episode,\"season_number\":$seasonNumber,\"name\":\"Episode $episode\"}"
            })
            append("]}")
        }
    }
}
