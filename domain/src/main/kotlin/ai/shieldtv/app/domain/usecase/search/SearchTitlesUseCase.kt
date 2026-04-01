package ai.shieldtv.app.domain.usecase.search

import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.domain.repository.MetadataRepository

class SearchTitlesUseCase(
    private val metadataRepository: MetadataRepository
) {
    suspend operator fun invoke(query: String): List<SearchResult> {
        return metadataRepository.search(query)
    }
}
