package ai.shieldtv.app.integration.scrapers.provider.sample

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.integration.scrapers.provider.ProviderQuerySanitizer
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderQueryBuilder
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest

class SampleProviderQueryBuilder : ProviderQueryBuilder {
    override fun build(request: SourceSearchRequest): ProviderRequest {
        return ProviderRequest(
            query = ProviderQuerySanitizer.toQuery(request),
            params = mapOf("q" to ProviderQuerySanitizer.toQuery(request))
        )
    }
}
