package ai.shieldtv.app.integration.scrapers.provider.knaben

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport

class KnabenTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        if (!KnabenConfig.isEnabled()) return ""
        val url = request.params["url"] ?: return ""
        val httpClient = HttpClientFactory.createDefault()
        return runCatching {
            httpClient.get(
                url,
                headers = request.headers + mapOf(
                    "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                )
            )
        }.getOrElse {
            ""
        }
    }
}
