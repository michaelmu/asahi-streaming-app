package ai.shieldtv.app.integration.scrapers.provider.torrentio

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TorrentioTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        if (!TorrentioConfig.isEnabled()) {
            return "{\"streams\": []}"
        }
        val path = request.params["path"] ?: return "{\"streams\": []}"
        val httpClient = HttpClientFactory.createDefault()
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(TorrentioConfig.baseUrl().trimEnd('/') + path))
            .GET()
            .build()
        return runCatching {
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body()
        }.getOrElse {
            "{\"streams\": []}"
        }
    }
}
