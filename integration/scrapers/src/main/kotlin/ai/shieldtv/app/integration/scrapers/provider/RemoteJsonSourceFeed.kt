package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.core.network.http.HttpClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RemoteJsonSourceFeed(
    private val baseUrlProvider: () -> String?,
    private val httpClient: HttpClient
) {
    suspend fun load(query: String, request: SourceSearchRequest): String {
        val baseUrl = baseUrlProvider()?.trim()?.trimEnd('/') ?: return DebugJsonSourceFeed.load(query, request)
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val requestUrl = "$baseUrl?query=$encodedQuery"
        return runCatching {
            httpClient.get(requestUrl)
        }.getOrElse {
            DebugJsonSourceFeed.load(query, request)
        }
    }
}
