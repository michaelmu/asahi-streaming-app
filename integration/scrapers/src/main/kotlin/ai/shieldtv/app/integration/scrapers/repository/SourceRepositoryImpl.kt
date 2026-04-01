package ai.shieldtv.app.integration.scrapers.repository

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.repository.SourceRepository

class SourceRepositoryImpl : SourceRepository {
    override suspend fun findSources(request: SourceSearchRequest): List<SourceResult> {
        return emptyList()
    }
}
