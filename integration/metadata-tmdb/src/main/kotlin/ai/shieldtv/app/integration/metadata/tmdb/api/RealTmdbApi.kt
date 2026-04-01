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
}
