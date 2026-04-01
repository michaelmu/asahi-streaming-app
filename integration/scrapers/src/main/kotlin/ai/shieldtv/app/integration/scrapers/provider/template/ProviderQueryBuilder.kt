package ai.shieldtv.app.integration.scrapers.provider.template

import ai.shieldtv.app.core.model.source.SourceSearchRequest

interface ProviderQueryBuilder {
    fun build(request: SourceSearchRequest): ProviderRequest
}
