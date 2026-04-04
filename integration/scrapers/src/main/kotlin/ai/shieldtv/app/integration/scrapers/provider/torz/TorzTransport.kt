package ai.shieldtv.app.integration.scrapers.provider.torz

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport

class TorzTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        if (!TorzConfig.isEnabled()) return "{\"data\":{\"items\":[]}}"
        val path = request.params["path"] ?: return "{\"data\":{\"items\":[]}}"
        val url = TorzConfig.baseUrl().trimEnd('/') + path
        val httpClient = HttpClientFactory.createDefault()
        return runCatching {
            httpClient.get(
                url,
                headers = request.headers + mapOf("User-Agent" to "Mozilla/5.0")
            )
        }.getOrElse {
            "{\"data\":{\"items\":[]}}"
        }
    }
}
