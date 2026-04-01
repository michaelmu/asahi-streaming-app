package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceSearchRequest

object DebugJsonSourceFeed {
    suspend fun load(query: String, request: SourceSearchRequest): String {
        return """
            {
              "results": [
                {
                  "providerId": "json-demo",
                  "title": "${query} 4K WEB",
                  "url": "https://example.com/json-source/${request.mediaRef.title}",
                  "infoHash": "",
                  "sizeBytes": 4800000000,
                  "seeders": 88,
                  "qualityHint": "4k"
                }
              ]
            }
        """.trimIndent()
    }
}
