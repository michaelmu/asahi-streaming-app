package ai.shieldtv.app.integration.scrapers.provider.real

import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderParser
import org.json.JSONArray
import org.json.JSONObject

class RealProviderParser : ProviderParser {
    override fun parse(rawResponse: String): List<RawProviderSource> {
        return runCatching {
            val root = JSONObject(rawResponse)
            val items = root.optJSONArray("results") ?: JSONArray()
            val mapped = mutableListOf<RawProviderSource>()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val title = item.optString("title")
                val url = item.optString("url")
                if (title.isBlank() || url.isBlank()) continue
                mapped += RawProviderSource(
                    providerId = item.optString("providerId").ifBlank { "real-provider" },
                    title = title,
                    url = url,
                    infoHash = item.optString("infoHash").ifBlank { null },
                    sizeBytes = item.optLong("sizeBytes").takeIf { it > 0 },
                    seeders = item.optInt("seeders").takeIf { it > 0 },
                    extra = buildMap {
                        item.optString("qualityHint").takeIf { it.isNotBlank() }?.let { put("quality_hint", it) }
                        item.optString("transport").takeIf { it.isNotBlank() }?.let { put("transport", it) }
                    }
                )
            }
            mapped
        }.getOrElse { emptyList() }
    }
}
