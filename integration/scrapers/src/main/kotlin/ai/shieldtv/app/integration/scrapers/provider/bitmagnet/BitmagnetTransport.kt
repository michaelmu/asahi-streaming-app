package ai.shieldtv.app.integration.scrapers.provider.bitmagnet

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport

class BitmagnetTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        if (!BitmagnetConfig.isEnabled()) return "<?xml version=\"1.0\"?><rss/>"
        val path = request.params["path"] ?: return "<?xml version=\"1.0\"?><rss/>"
        val url = BitmagnetConfig.baseUrl().trimEnd('/') + path
        val httpClient = HttpClientFactory.createDefault()
        return runCatching {
            httpClient.get(
                url,
                headers = request.headers + mapOf("User-Agent" to "Mozilla/5.0")
            )
        }.getOrElse {
            "<?xml version=\"1.0\"?><rss/>"
        }
    }
}
