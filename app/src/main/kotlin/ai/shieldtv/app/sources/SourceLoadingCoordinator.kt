package ai.shieldtv.app.sources

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.feature.sources.presentation.SourcesViewModel
import ai.shieldtv.app.settings.SourcePreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.LifecycleCoroutineScope

data class SourceLoadRequest(
    val mediaRef: MediaRef,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val authLinked: Boolean,
    val preferences: SourcePreferences,
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
    private val sourcesViewModel: SourcesViewModel
) {
    private var loadJob: Job? = null
    private val providerProgress = linkedMapOf<String, SourceFetchProgress>()

    fun currentProgress(): List<SourceFetchProgress> = providerProgress.values.toList()

    fun cancel() {
        loadJob?.cancel()
        loadJob = null
    }

    fun load(
        request: SourceLoadRequest,
        onStarted: () -> Unit,
        onProgressUpdated: (List<SourceFetchProgress>) -> Unit,
        onCompleted: (SourceLoadResult) -> Unit
    ) {
        cancel()
        providerProgress.clear()
        onStarted()

        loadJob = lifecycleScope.launch {
            val state = sourcesViewModel.load(
                request = SourceSearchRequest(
                    mediaRef = request.mediaRef,
                    seasonNumber = request.seasonNumber,
                    episodeNumber = request.episodeNumber,
                    filters = request.filters
                ),
                enabledProviderIds = request.preferences.enabledProviders,
                onProgress = { progress ->
                    providerProgress[progress.providerId] = progress
                    onProgressUpdated(currentProgress())
                }
            )

            val filteredSources = state.sources.filter { source ->
                source.debridService != DebridService.NONE || request.authLinked
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
