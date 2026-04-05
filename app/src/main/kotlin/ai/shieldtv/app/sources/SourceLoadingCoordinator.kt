package ai.shieldtv.app.sources

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.domain.source.SourceEligibilityPolicy
import ai.shieldtv.app.feature.sources.presentation.SourcesViewModel
import ai.shieldtv.app.settings.SourcePreferences
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class SourceLoadRequest(
    val mediaRef: MediaRef,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val authLinked: Boolean,
    val preferences: SourcePreferences,
    val availableProviderIds: Set<String>,
    val filters: SourceFilters,
    val searchLabel: String
)

data class SourceLoadResult(
    val sources: List<SourceResult>,
    val diagnostics: String?,
    val error: String?
)

class SourceLoadingCoordinator(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val sourcesViewModel: SourcesViewModel,
    private val sourceEligibilityPolicy: SourceEligibilityPolicy = SourceEligibilityPolicy()
) {
    private var loadJob: Job? = null
    private val providerProgress = linkedMapOf<String, SourceFetchProgress>()
    private val partialSources = linkedMapOf<String, SourceResult>()

    fun currentProgress(): List<SourceFetchProgress> = providerProgress.values.toList()
    fun currentSources(): List<SourceResult> = partialSources.values.toList()

    fun cancel() {
        loadJob?.cancel()
        loadJob = null
    }

    fun load(
        request: SourceLoadRequest,
        onStarted: () -> Unit,
        onProgressUpdated: (List<SourceFetchProgress>) -> Unit,
        onIncrementalUpdate: ((IncrementalSourceUpdate) -> Unit)? = null,
        onCompleted: (SourceLoadResult) -> Unit
    ) {
        cancel()
        providerProgress.clear()
        partialSources.clear()
        val enabledProviders = request.preferences.providerSelection.effectiveEnabledProviders(request.availableProviderIds)
        val totalProviders = enabledProviders.ifEmpty { request.availableProviderIds }.size
        val sourceCacheStore = runCatching { AppContainer.sourceListCacheStore }.getOrNull()
        val cacheKey = sourceCacheStore?.keyFor(
            mediaRef = request.mediaRef,
            seasonNumber = request.seasonNumber,
            episodeNumber = request.episodeNumber,
            authLinked = request.authLinked
        )
        if (sourceCacheStore != null && cacheKey != null) {
            sourceCacheStore.load(cacheKey, maxAgeMs = 30L * 60L * 1000L)?.takeIf { it.isNotEmpty() }?.let { cachedSources ->
                partialSources.putAll(cachedSources.associateBy { it.id })
                onStarted()
                onCompleted(
                    SourceLoadResult(
                        sources = cachedSources,
                        diagnostics = "source_cache=hit | results=${cachedSources.size}",
                        error = null
                    )
                )
                return
            }
        }
        onStarted()

        loadJob = lifecycleScope.launch {
            val state = sourcesViewModel.load(
                request = SourceSearchRequest(
                    mediaRef = request.mediaRef,
                    seasonNumber = request.seasonNumber,
                    episodeNumber = request.episodeNumber,
                    filters = request.filters
                ),
                enabledProviderIds = enabledProviders,
                onProgress = { progress ->
                    providerProgress[progress.providerId] = progress
                    onProgressUpdated(currentProgress())
                },
                onIncrementalResults = { incremental ->
                    partialSources.clear()
                    sourceEligibilityPolicy.filterForAuth(incremental.sources, request.authLinked)
                        .forEach { partialSources[it.id] = it }
                    onIncrementalUpdate?.invoke(
                        IncrementalSourceUpdate(
                            sources = currentSources(),
                            progress = currentProgress(),
                            completedProviders = incremental.completedProviders,
                            totalProviders = totalProviders
                        )
                    )
                }
            )

            val filteredSources = sourceEligibilityPolicy.filterForAuth(
                sources = state.sources,
                authLinked = request.authLinked
            )
            partialSources.clear()
            filteredSources.forEach { partialSources[it.id] = it }

            if (sourceCacheStore != null && cacheKey != null) {
                sourceCacheStore.save(cacheKey, filteredSources)
            }
            onCompleted(
                SourceLoadResult(
                    sources = filteredSources,
                    diagnostics = state.diagnostics,
                    error = state.error
                )
            )
        }
    }
}
