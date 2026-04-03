package ai.shieldtv.app.integration.scrapers.provider.sample

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.ProviderCapabilities
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceProvider
import ai.shieldtv.app.integration.scrapers.provider.template.TemplateBackedSourceProviderAdapter

class SampleTemplateSourceProvider : SourceProvider {
    private val adapter = TemplateBackedSourceProviderAdapter(
        queryBuilder = SampleProviderQueryBuilder(),
        transport = SampleProviderTransport(),
        parser = SampleProviderParser()
    )

    override val id: String = "sample-template"
    override val displayName: String = "Sample Template Provider"
    override val kind: ProviderKind = ProviderKind.SCRAPER
    override val capabilities: ProviderCapabilities = ProviderCapabilities(productionReady = false)

    override suspend fun search(request: SourceSearchRequest): List<RawProviderSource> {
        return adapter.fetch("", request)
    }
}
