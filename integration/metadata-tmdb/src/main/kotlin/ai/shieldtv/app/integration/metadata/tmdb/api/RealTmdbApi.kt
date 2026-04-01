package ai.shieldtv.app.integration.metadata.tmdb.api

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class RealTmdbApi(
    private val apiKeyProvider: () -> String?,
    private val httpClient: HttpClient
) : TmdbApi {
    override suspend fun searchMulti(query: String): String {
        val apiKey = apiKeyProvider() ?: return query
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=$encodedQuery"))
            .GET()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }

    override suspend fun getDetails(tmdbId: String, mediaType: String): String {
        val apiKey = apiKeyProvider() ?: return "{}"
        val path = when (mediaType.lowercase()) {
            "movie" -> "movie"
            "tv", "show" -> "tv"
            else -> return "{}"
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.themoviedb.org/3/$path/$tmdbId?api_key=$apiKey"))
            .GET()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
    }
}
