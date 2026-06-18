package ai.codexa.app.domain.repository

import ai.codexa.app.core.model.source.SourceResult
import ai.codexa.app.core.model.source.SourceSearchRequest

interface SourceRepository {
    suspend fun findSources(request: SourceSearchRequest): List<SourceResult>
}
