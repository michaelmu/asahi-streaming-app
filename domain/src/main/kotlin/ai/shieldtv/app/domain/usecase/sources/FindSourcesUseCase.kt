package ai.shieldtv.app.domain.usecase.sources

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.repository.IncrementalSourceResult
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.domain.repository.SourceRepository

class FindSourcesUseCase(
    private val sourceRepository: SourceRepository
) {
    suspend operator fun invoke(
        request: SourceSearchRequest,
        enabledProviderIds: Set<String> = emptySet(),
        onProgress: ((SourceFetchProgress) -> Unit)? = null,
        onIncrementalResults: ((IncrementalSourceResult) -> Unit)? = null
    ): List<SourceResult> {
        return sourceRepository.findSources(request, enabledProviderIds, onProgress, onIncrementalResults)
    }
}
