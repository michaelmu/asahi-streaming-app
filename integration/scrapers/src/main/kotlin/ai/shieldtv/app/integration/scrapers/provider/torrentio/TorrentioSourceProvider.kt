package ai.shieldtv.app.integration.scrapers.provider.torrentio

import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.domain.provider.SourceProvider
import ai.shieldtv.app.integration.scrapers.provider.template.TemplateBackedSourceProviderAdapter

class TorrentioSourceProvider : SourceProvider {
    private val adapter = TemplateBackedSourceProviderAdapter(
        queryBuilder = TorrentioQueryBuilder(),
        transport = TorrentioTransport(),
        parser = TorrentioParser()
    )

    override val id: String = "torrentio"
    override val displayName: String = "Torrentio"
    override val kind: ProviderKind = ProviderKind.SCRAPER

    override suspend fun search(request: SourceSearchRequest): List<RawProviderSource> {
        return adapter.fetch("", request)
    }
}
