package ai.shieldtv.app.integration.scrapers.provider.torrentio

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenProvider
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport
import java.net.URLEncoder

class TorrentioTransport(
    private val tokenProvider: RealDebridTokenProvider? = null
) : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        if (!TorrentioConfig.isEnabled()) {
            return "{\"streams\": []}"
        }
        val path = request.params["path"] ?: return "{\"streams\": []}"
        val httpClient = HttpClientFactory.createDefault()
        val token = tokenProvider?.getAccessToken()?.takeIf { it.isNotBlank() }
        val url = buildUrl(path, token)
        RealDebridDebugState.lastTorrentioUrl = url
        return runCatching {
            httpClient.get(url, headers = request.headers).also { response ->
                RealDebridDebugState.lastTorrentioResponsePreview = response.take(500)
            }
        }.getOrElse {
            RealDebridDebugState.lastTorrentioResponsePreview = "transport_error:${it.message}"
            "{\"streams\": []}"
        }
    }

    private fun buildUrl(path: String, token: String?): String {
        val base = TorrentioConfig.baseUrl().trimEnd('/')
        if (token.isNullOrBlank()) return base + path

        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val normalizedPath = path.removePrefix("/")
        return "$base/realdebrid=$encodedToken/$normalizedPath"
    }
}
