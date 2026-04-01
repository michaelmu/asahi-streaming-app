package ai.shieldtv.app.integration.scrapers.provider.template

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.integration.scrapers.provider.SourceProviderAdapter

class TemplateBackedSourceProviderAdapter(
    private val queryBuilder: ProviderQueryBuilder,
    private val transport: ProviderTransport,
    private val parser: ProviderParser
) : SourceProviderAdapter {
    override suspend fun fetch(query: String, request: SourceSearchRequest): List<RawProviderSource> {
        val providerRequest = queryBuilder.build(request)
        val rawResponse = transport.fetch(providerRequest)
        return parser.parse(rawResponse)
    }
}
