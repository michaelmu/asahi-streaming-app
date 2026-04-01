package ai.shieldtv.app.domain.provider

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest

interface SourceProvider {
    val id: String
    val displayName: String
    val kind: ProviderKind

    suspend fun search(request: SourceSearchRequest): List<RawProviderSource>
}
