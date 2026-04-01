package ai.shieldtv.app.integration.scrapers.provider.torrentio

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport

class TorrentioTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        if (!TorrentioConfig.isEnabled()) {
            return "{\"streams\": []}"
        }
        val path = request.params["path"] ?: return "{\"streams\": []}"
        val httpClient = HttpClientFactory.createDefault()
        return runCatching {
            httpClient.get(TorrentioConfig.baseUrl().trimEnd('/') + path)
        }.getOrElse {
            "{\"streams\": []}"
        }
    }
}
