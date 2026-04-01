package ai.shieldtv.app.domain.usecase.sources

import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.DebridRepository

class ResolveSourceUseCase(
    private val debridRepository: DebridRepository
) {
    suspend operator fun invoke(source: SourceResult): ResolvedStream {
        return debridRepository.resolve(source)
    }
}
