package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.RawProviderSource

interface SourceProviderAdapter {
    suspend fun fetch(query: String, request: SourceSearchRequest): List<RawProviderSource>
}
