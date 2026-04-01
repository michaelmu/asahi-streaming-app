package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.domain.provider.RawProviderSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real JSON-backed adapter shape.
 *
 * For now this reads from a supplied JSON provider so we can prove the transport
 * parsing path before wiring a live remote endpoint.
 */
class JsonSourceProviderAdapter(
    private val jsonProvider: suspend (String, SourceSearchRequest) -> String
) : SourceProviderAdapter {
    override suspend fun fetch(query: String, request: SourceSearchRequest): List<RawProviderSource> {
        if (query.isBlank()) return emptyList()
        val json = jsonProvider(query, request)
        return parse(json)
    }

    private fun parse(json: String): List<RawProviderSource> {
        return runCatching {
            val root = JSONObject(json)
            val items = root.optJSONArray("results") ?: JSONArray()
            val mapped = mutableListOf<RawProviderSource>()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val title = item.optString("title")
                val url = item.optString("url")
                if (title.isBlank() || url.isBlank()) continue
                mapped += RawProviderSource(
                    providerId = item.optString("providerId").ifBlank { "json" },
                    title = title,
                    url = url,
                    infoHash = item.optString("infoHash").ifBlank { null },
                    sizeBytes = item.optLong("sizeBytes").takeIf { it > 0 },
                    seeders = item.optInt("seeders").takeIf { it > 0 },
                    extra = buildMap {
                        item.optString("qualityHint").takeIf { it.isNotBlank() }?.let { put("quality_hint", it) }
                        item.optString("transport").takeIf { it.isNotBlank() }?.let { put("transport", it) } ?: put("transport", "json")
                        item.optString("query").takeIf { it.isNotBlank() }?.let { put("query", it) }
                    }
                )
            }
            mapped
        }.getOrElse {
            emptyList()
        }
    }
}
