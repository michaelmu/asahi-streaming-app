package ai.shieldtv.app.integration.scrapers.provider.real

import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport

/**
 * First real-provider transport skeleton.
 *
 * This intentionally returns a controlled response until a concrete upstream is chosen.
 */
class RealProviderTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        return if (RealProviderConfig.isEnabled() && RealProviderConfig.baseUrl() != null) {
            """
                {
                  "results": [
                    {
                      "providerId": "real-provider",
                      "title": "${request.query} REAL 1080p",
                      "url": "${RealProviderConfig.baseUrl()}/stream/${request.query}",
                      "sizeBytes": 3200000000,
                      "seeders": 55,
                      "qualityHint": "1080p",
                      "transport": "real-provider-skeleton"
                    }
                  ]
                }
            """.trimIndent()
        } else {
            """
                {
                  "results": []
                }
            """.trimIndent()
        }
    }
}
