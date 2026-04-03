package ai.shieldtv.app.integration.scrapers.provider.comet

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport

class CometTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        if (!CometConfig.isEnabled()) return "{\"streams\": []}"
        val path = request.params["path"] ?: return "{\"streams\": []}"
        val url = CometConfig.baseUrl().trimEnd('/') + path
        val httpClient = HttpClientFactory.createDefault()
        return runCatching {
            httpClient.get(url, headers = request.headers)
        }.getOrElse {
            "{\"streams\": []}"
        }
    }
}
