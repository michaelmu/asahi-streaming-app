package ai.shieldtv.app.integration.scrapers.provider.sample

import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderTransport

class SampleProviderTransport : ProviderTransport {
    override suspend fun fetch(request: ProviderRequest): String {
        return """
            {
              "results": [
                {
                  "providerId": "sample-template",
                  "title": "${request.query} 1080p WEB",
                  "url": "https://example.com/sample/${request.query}",
                  "sizeBytes": 2100000000,
                  "seeders": 25,
                  "qualityHint": "1080p"
                }
              ]
            }
        """.trimIndent()
    }
}
