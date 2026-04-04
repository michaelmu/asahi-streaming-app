package ai.shieldtv.app.integration.scrapers.repository

import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.SourceNormalizer
import ai.shieldtv.app.domain.repository.SourceRepository
import ai.shieldtv.app.domain.source.ranking.SourceCacheMarker
import ai.shieldtv.app.domain.source.ranking.SourceRanker
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import ai.shieldtv.app.integration.scrapers.provider.ProviderModeDecider
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry

class SourceRepositoryImpl(
    private val providerRegistry: ProviderRegistry,
    private val sourceNormalizer: SourceNormalizer,
    private val sourceRanker: SourceRanker,
    private val sourceCacheMarker: SourceCacheMarker? = null
) : SourceRepository {
    override suspend fun findSources(
        request: SourceSearchRequest,
        onProgress: ((SourceFetchProgress) -> Unit)?
    ): List<SourceResult> {
        RealDebridDebugState.lastSourceRepositorySeen = "yes"
        RealDebridDebugState.lastSourceRepositoryMarkerPresent = if (sourceCacheMarker == null) "no" else "yes"
        val providerSummaries = mutableListOf<String>()
        val rawResults = providerRegistry.activeProviders().flatMap { provider ->
            onProgress?.invoke(
                SourceFetchProgress(
                    providerId = provider.id,
                    providerDisplayName = provider.displayName,
                    state = SourceFetchProgress.State.STARTED
                )
            )
            runCatching {
                provider.search(request).map { raw ->
                    sourceNormalizer.normalize(request, provider, raw)
                }
            }.onSuccess { normalized ->
                providerSummaries += "${provider.id}:${normalized.size}"
                onProgress?.invoke(
                    SourceFetchProgress(
                        providerId = provider.id,
                        providerDisplayName = provider.displayName,
                        state = SourceFetchProgress.State.COMPLETED,
                        resultCount = normalized.size
                    )
                )
            }.onFailure { error ->
                providerSummaries += "${provider.id}:error"
                onProgress?.invoke(
                    SourceFetchProgress(
                        providerId = provider.id,
                        providerDisplayName = provider.displayName,
                        state = SourceFetchProgress.State.FAILED,
                        message = error.message
                    )
                )
            }.getOrElse { emptyList() }
        }
        RealDebridDebugState.lastSourceProviderSummary = providerSummaries.joinToString(",")
        val cacheMarked = sourceCacheMarker?.markCached(rawResults) ?: rawResults
        val shaped = ProviderModeDecider.shapeSources(cacheMarked)
        RealDebridDebugState.lastSourceLiveCount = shaped.size.toString()
        RealDebridDebugState.lastSourceFallbackCount = "0"
        println(
            buildString {
                append("AsahiSources ")
                append("title=")
                append(request.mediaRef.title)
                append(" type=")
                append(request.mediaRef.mediaType)
                request.seasonNumber?.let {
                    append(" season=")
                    append(it)
                }
                request.episodeNumber?.let {
                    append(" episode=")
                    append(it)
                }
                append(" providers=")
                append(RealDebridDebugState.lastSourceProviderSummary)
                append(" resultCount=")
                append(shaped.size)
            }
        )
        return sourceRanker.rank(shaped, SourceFilters())
    }
}
