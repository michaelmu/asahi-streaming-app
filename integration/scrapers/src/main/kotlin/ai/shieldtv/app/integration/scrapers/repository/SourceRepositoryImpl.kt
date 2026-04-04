package ai.shieldtv.app.integration.scrapers.repository

import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.SourceNormalizer
import ai.shieldtv.app.domain.repository.IncrementalSourceResult
import ai.shieldtv.app.domain.repository.SourceFetchError
import ai.shieldtv.app.domain.repository.SourceRepository
import ai.shieldtv.app.domain.source.ranking.SourceCacheMarker
import ai.shieldtv.app.domain.source.ranking.SourceRanker
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import ai.shieldtv.app.integration.scrapers.provider.ProviderModeDecider
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.withTimeout

class SourceRepositoryImpl(
    private val providerRegistry: ProviderRegistry,
    private val sourceNormalizer: SourceNormalizer,
    private val sourceRanker: SourceRanker,
    private val sourceCacheMarker: SourceCacheMarker? = null,
    private val providerTimeoutMs: Long = 15_000L
) : SourceRepository {
    override suspend fun findSources(
        request: SourceSearchRequest,
        enabledProviderIds: Set<String>,
        onProgress: ((SourceFetchProgress) -> Unit)?,
        onIncrementalResults: ((IncrementalSourceResult) -> Unit)?
    ): List<SourceResult> {
        RealDebridDebugState.lastSourceRepositorySeen = "yes"
        RealDebridDebugState.lastSourceRepositoryMarkerPresent = if (sourceCacheMarker == null) "no" else "yes"
        val providers = providerRegistry.activeProviders(enabledProviderIds = enabledProviderIds)
        val aggregatedNormalized = mutableListOf<SourceResult>()
        val completedProviders = AtomicInteger(0)
        val providerResults = coroutineScope {
            providers.map { provider ->
                async {
                    val startedAt = System.currentTimeMillis()
                    onProgress?.invoke(
                        SourceFetchProgress(
                            providerId = provider.id,
                            providerDisplayName = provider.displayName,
                            state = SourceFetchProgress.State.STARTED
                        )
                    )
                    try {
                        val normalized = withTimeout(providerTimeoutMs) {
                            provider.search(request).map { raw ->
                                sourceNormalizer.normalize(request, provider, raw)
                            }
                        }
                        val latency = System.currentTimeMillis() - startedAt
                        onProgress?.invoke(
                            SourceFetchProgress(
                                providerId = provider.id,
                                providerDisplayName = provider.displayName,
                                state = SourceFetchProgress.State.COMPLETED,
                                resultCount = normalized.size,
                                latencyMs = latency
                            )
                        )
                        synchronized(aggregatedNormalized) {
                            aggregatedNormalized += normalized
                            val shapedPartial = ProviderModeDecider.shapeSources(aggregatedNormalized.toList())
                            val rankedPartial = sourceRanker.rank(shapedPartial, SourceFilters())
                            onIncrementalResults?.invoke(
                                IncrementalSourceResult(
                                    sources = rankedPartial,
                                    completedProviders = completedProviders.incrementAndGet(),
                                    totalProviders = providers.size
                                )
                            )
                        }
                        ProviderFetchResult(
                            summary = "${provider.id}:${normalized.size}",
                            normalized = normalized
                        )
                    } catch (error: Throwable) {
                        val latency = System.currentTimeMillis() - startedAt
                        val errorType = when (error) {
                            is TimeoutCancellationException -> SourceFetchError.Timeout::class.simpleName
                            else -> SourceFetchError.ProviderFailure::class.simpleName
                        }
                        onProgress?.invoke(
                            SourceFetchProgress(
                                providerId = provider.id,
                                providerDisplayName = provider.displayName,
                                state = SourceFetchProgress.State.FAILED,
                                message = error.message,
                                latencyMs = latency,
                                errorType = errorType
                            )
                        )
                        onIncrementalResults?.invoke(
                            IncrementalSourceResult(
                                sources = sourceRanker.rank(ProviderModeDecider.shapeSources(aggregatedNormalized.toList()), SourceFilters()),
                                completedProviders = completedProviders.incrementAndGet(),
                                totalProviders = providers.size
                            )
                        )
                        ProviderFetchResult(
                            summary = "${provider.id}:error",
                            normalized = emptyList()
                        )
                    }
                }
            }.map { it.await() }
        }
        RealDebridDebugState.lastSourceProviderSummary = providerResults.joinToString(",") { it.summary }
        val rawResults = providerResults.flatMap { it.normalized }
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

    private data class ProviderFetchResult(
        val summary: String,
        val normalized: List<SourceResult>
    )
}
