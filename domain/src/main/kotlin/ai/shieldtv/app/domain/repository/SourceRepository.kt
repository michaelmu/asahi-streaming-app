package ai.shieldtv.app.domain.repository

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest

interface SourceRepository {
    suspend fun findSources(
        request: SourceSearchRequest,
        onProgress: ((SourceFetchProgress) -> Unit)? = null
    ): List<SourceResult>
}
