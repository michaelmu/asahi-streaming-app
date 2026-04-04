package ai.shieldtv.app.integration.metadata.tmdb.api

import ai.shieldtv.app.core.network.http.HttpClient
import java.net.URLEncoder

class RealTmdbApi(
    private val apiKeyProvider: () -> String?,
    private val httpClient: HttpClient
) : TmdbApi {
    override suspend fun searchMulti(query: String): String {
        val apiKey = apiKeyProvider() ?: return query
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        return httpClient.get(
            url = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=$encodedQuery"
        )
    }

    override suspend fun getDetails(tmdbId: String, mediaType: String): String {
        val apiKey = apiKeyProvider() ?: return "{}"
        val path = when (mediaType.lowercase()) {
            "movie" -> "movie"
            "tv", "show" -> "tv"
            else -> return "{}"
        }
        return httpClient.get(
            url = "https://api.themoviedb.org/3/$path/$tmdbId?api_key=$apiKey&append_to_response=external_ids"
        )
    }

    override suspend fun getSeasonDetails(tmdbId: String, seasonNumber: Int): String {
        val apiKey = apiKeyProvider() ?: return "{}"
        return httpClient.get(
            url = "https://api.themoviedb.org/3/tv/$tmdbId/season/$seasonNumber?api_key=$apiKey"
        )
    }
}
