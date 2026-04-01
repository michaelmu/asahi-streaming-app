package ai.shieldtv.app.domain.provider

import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest

interface SourceNormalizer {
    fun normalize(
        request: SourceSearchRequest,
        provider: SourceProvider,
        raw: RawProviderSource
    ): SourceResult
}
