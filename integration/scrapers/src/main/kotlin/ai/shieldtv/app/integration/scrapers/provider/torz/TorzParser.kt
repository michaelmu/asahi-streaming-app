package ai.shieldtv.app.integration.scrapers.provider.torz

import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.integration.scrapers.normalize.ReleaseInfoParser
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderParser
import org.json.JSONObject

class TorzParser : ProviderParser {
    override fun parse(rawResponse: String): List<RawProviderSource> {
        if (rawResponse.isBlank()) return emptyList()
        return runCatching {
            val root = JSONObject(rawResponse)
            val items = root.optJSONObject("data")?.optJSONArray("items") ?: return@runCatching emptyList()
            val results = mutableListOf<RawProviderSource>()
            for (index in 0 until items.length()) {
                val file = items.optJSONObject(index) ?: continue
                val infoHash = file.optString("hash")
                if (infoHash.isBlank()) continue
                val name = file.optString("name")
                if (name.isBlank()) continue
                val parsedRelease = ReleaseInfoParser.parse(name)
                if (parsedRelease.probablyPack) continue
                results += RawProviderSource(
                    providerId = "torz",
                    title = name,
                    url = "magnet:?xt=urn:btih:$infoHash&dn=$name",
                    infoHash = infoHash.lowercase(),
                    sizeBytes = file.optDouble("size").takeIf { it > 0 }?.toLong(),
                    seeders = file.optInt("seeders").takeIf { it > 0 },
                    extra = buildMap {
                        put("transport", "torz")
                        put("debrid", "realdebrid")
                        put("quality_hint", when (parsedRelease.quality) {
                            ai.shieldtv.app.core.model.source.Quality.UHD_4K -> "4k"
                            ai.shieldtv.app.core.model.source.Quality.FHD_1080P -> "1080p"
                            ai.shieldtv.app.core.model.source.Quality.HD_720P -> "720p"
                            ai.shieldtv.app.core.model.source.Quality.CAM -> "cam"
                            else -> "sd"
                        })
                        if (parsedRelease.flags.isNotEmpty()) {
                            put("flags", parsedRelease.flags.joinToString(","))
                        }
                    }
                )
            }
            results
        }.getOrElse {
            emptyList()
        }
    }
}
