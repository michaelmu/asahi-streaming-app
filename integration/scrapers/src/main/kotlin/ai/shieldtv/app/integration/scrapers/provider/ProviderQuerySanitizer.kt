package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceSearchRequest

object ProviderQuerySanitizer {
    fun toQuery(request: SourceSearchRequest): String {
        return buildString {
            append(request.mediaRef.title)
            request.mediaRef.year?.let {
                append(' ')
                append(it)
            }
            request.seasonNumber?.let {
                append(" S")
                append(it.toString().padStart(2, '0'))
            }
            request.episodeNumber?.let {
                append("E")
                append(it.toString().padStart(2, '0'))
            }
        }.trim()
    }
}
