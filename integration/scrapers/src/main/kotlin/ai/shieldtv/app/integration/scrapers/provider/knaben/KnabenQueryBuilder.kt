package ai.shieldtv.app.integration.scrapers.provider.knaben

import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.integration.scrapers.provider.ProviderQuerySanitizer
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderQueryBuilder
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import java.net.URLEncoder

class KnabenQueryBuilder : ProviderQueryBuilder {
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
        val cat = when (request.mediaRef.mediaType) {
            MediaType.MOVIE -> "003000000"
            MediaType.SHOW, MediaType.EPISODE, MediaType.SEASON -> "002000000"
        }
        val encoded = URLEncoder.encode(query, "UTF-8")
        return ProviderRequest(
            query = query,
            params = mapOf("url" to "${KnabenConfig.baseUrl()}/search/index.php?cat=$cat&q=$encoded&search=fast")
        )
    }
}
