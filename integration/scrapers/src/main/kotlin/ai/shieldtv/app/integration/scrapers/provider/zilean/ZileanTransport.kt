package ai.shieldtv.app.integration.scrapers.provider.zilean

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport

class ZileanTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        if (!ZileanConfig.isEnabled()) return "[]"
        val path = request.params["path"] ?: return "[]"
        val url = ZileanConfig.baseUrl().trimEnd('/') + path
        val httpClient = HttpClientFactory.createDefault()
        return runCatching {
            httpClient.get(
                url,
                headers = request.headers + mapOf("User-Agent" to "Mozilla/5.0")
            )
        }.getOrElse {
            "[]"
        }
    }
}
