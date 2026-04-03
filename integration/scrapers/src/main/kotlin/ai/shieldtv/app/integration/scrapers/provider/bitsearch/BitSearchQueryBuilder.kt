package ai.shieldtv.app.integration.scrapers.provider.bitsearch

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.integration.scrapers.provider.ProviderQuerySanitizer
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderQueryBuilder
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class BitSearchQueryBuilder : ProviderQueryBuilder {
    override fun build(request: SourceSearchRequest): ProviderRequest {
        val suffix = when {
            request.episodeNumber != null && request.seasonNumber != null ->
                "S${request.seasonNumber.toString().padStart(2, '0')}E${request.episodeNumber.toString().padStart(2, '0')}"
            request.mediaRef.year != null -> request.mediaRef.year.toString()
            else -> ""
        }
        val query = listOf(
            ProviderQuerySanitizer.toQuery(request),
            suffix.takeIf { it.isNotBlank() }
        ).filterNotNull().joinToString(" ")
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        return ProviderRequest(
            query = query,
            params = mapOf("url" to "${BitSearchConfig.baseUrl()}/search?q=$encoded&sort=size")
        )
    }
}
