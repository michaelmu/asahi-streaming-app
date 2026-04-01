package ai.shieldtv.app.integration.scrapers.repository

import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.SourceNormalizer
import ai.shieldtv.app.domain.repository.SourceRepository
import ai.shieldtv.app.domain.source.ranking.SourceRanker
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry

class SourceRepositoryImpl(
    private val providerRegistry: ProviderRegistry,
    private val sourceNormalizer: SourceNormalizer,
    private val sourceRanker: SourceRanker
) : SourceRepository {
    override suspend fun findSources(request: SourceSearchRequest): List<SourceResult> {
        val rawResults = providerRegistry.providers.flatMap { provider ->
            provider.search(request).map { raw ->
                sourceNormalizer.normalize(request, provider, raw)
            }
        }
        return sourceRanker.rank(rawResults, SourceFilters())
    }
}
