package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class RemoteJsonSourceFeed(
    private val baseUrlProvider: () -> String?,
    private val httpClient: HttpClient
) {
    suspend fun load(query: String, request: SourceSearchRequest): String {
        val baseUrl = baseUrlProvider()?.trim()?.trimEnd('/') ?: return DebugJsonSourceFeed.load(query, request)
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val requestUrl = "$baseUrl?query=$encodedQuery"
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(requestUrl))
            .GET()
            .build()
        return runCatching {
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body()
        }.getOrElse {
            DebugJsonSourceFeed.load(query, request)
        }
    }
}
