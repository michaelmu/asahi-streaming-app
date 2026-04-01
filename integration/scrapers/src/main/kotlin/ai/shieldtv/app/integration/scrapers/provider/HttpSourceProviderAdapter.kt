package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.RawProviderSource

/**
 * Transitional transport-shaped adapter.
 *
 * This is not a real scraper transport yet, but it gives the provider layer the
 * shape of an HTTP-backed adapter so the rest of the system can evolve without
 * being hard-wired to fake in-memory results.
 */
class HttpSourceProviderAdapter : SourceProviderAdapter {
    override suspend fun fetch(query: String, request: SourceSearchRequest): List<RawProviderSource> {
        if (query.isBlank()) return emptyList()
        return listOf(
            RawProviderSource(
                providerId = "http-fake",
                title = "$query 720p WEB",
                url = "https://example.com/http-source/${request.mediaRef.title}",
                infoHash = null,
                sizeBytes = 1_200_000_000,
                seeders = 12,
                extra = mapOf(
                    "quality_hint" to "720p",
                    "transport" to "http-shaped",
                    "query" to query
                )
            )
        )
    }
}
