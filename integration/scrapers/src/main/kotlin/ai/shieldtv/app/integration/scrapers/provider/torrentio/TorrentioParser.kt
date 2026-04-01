package ai.shieldtv.app.integration.scrapers.provider.torrentio

import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.integration.scrapers.normalize.ReleaseInfoParser
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderParser
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

class TorrentioParser : ProviderParser {
    override fun parse(rawResponse: String): List<RawProviderSource> {
        return runCatching {
            val root = JSONObject(rawResponse)
            val streams = root.optJSONArray("streams") ?: JSONArray()
            val mapped = mutableListOf<RawProviderSource>()
            for (index in 0 until streams.length()) {
                val item = streams.optJSONObject(index) ?: continue
                val titleBlock = item.optString("title")
                val titleLines = titleBlock.split("\n")
                val primaryTitle = titleLines.firstOrNull()?.trim().orEmpty()
                if (primaryTitle.isBlank() || looksLikeJunk(primaryTitle)) continue

                val parsedRelease = ReleaseInfoParser.parse(primaryTitle)
                if (parsedRelease.probablyPack) continue

                val resolvedUrl = item.optString("url")
                val infoHash = item.optString("infoHash").ifBlank {
                    extractInfoHash(resolvedUrl)
                }
                val url = resolvedUrl.ifBlank {
                    infoHash?.let { "magnet:?xt=urn:btih:$it&dn=${primaryTitle.ifBlank { "torrentio" }}" } ?: ""
                }
                if (url.isBlank()) continue

                val sizeBytes = extractSizeBytes(titleBlock)
                val seeders = extractSeeders(titleBlock)

                val name = item.optString("name")
                val debridAware = name.contains("[RD+]", ignoreCase = true) ||
                    name.contains("realdebrid", ignoreCase = true)

                mapped += RawProviderSource(
                    providerId = "torrentio",
                    title = primaryTitle,
                    url = url,
                    infoHash = infoHash,
                    sizeBytes = sizeBytes,
                    seeders = seeders,
                    extra = buildMap {
                        put("quality_hint", when (parsedRelease.quality) {
                            ai.shieldtv.app.core.model.source.Quality.UHD_4K -> "4k"
                            ai.shieldtv.app.core.model.source.Quality.FHD_1080P -> "1080p"
                            ai.shieldtv.app.core.model.source.Quality.HD_720P -> "720p"
                            ai.shieldtv.app.core.model.source.Quality.CAM -> "cam"
                            else -> "sd"
                        })
                        put("transport", "torrentio")
                        if (debridAware) {
                            put("debrid", "realdebrid")
                            put("cache_hint", "cached")
                        }
                        if (parsedRelease.flags.isNotEmpty()) put("flags", parsedRelease.flags.joinToString(","))
                    }
                )
            }
            mapped
        }.getOrElse { emptyList() }
    }

    private fun looksLikeJunk(title: String): Boolean {
        val lower = title.lowercase()
        return listOf("pokemon", "pack part", "amazing films", "paczka", "full series", "bonus content").any { it in lower }
    }

    private fun extractSeeders(text: String): Int? {
        val match = Pattern.compile("👤\\s*(\\d+)").matcher(text)
        return if (match.find()) match.group(1)?.toIntOrNull() else null
    }

    private fun extractSizeBytes(text: String): Long? {
        val match = Pattern.compile("💾\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(GB|MB|TB)", Pattern.CASE_INSENSITIVE).matcher(text)
        if (!match.find()) return null
        val value = match.group(1)?.toDoubleOrNull() ?: return null
        return when (match.group(2)?.uppercase()) {
            "TB" -> (value * 1024 * 1024 * 1024 * 1024).toLong()
            "GB" -> (value * 1024 * 1024 * 1024).toLong()
            "MB" -> (value * 1024 * 1024).toLong()
            else -> null
        }
    }

    private fun extractInfoHash(url: String): String? {
        val match = Pattern.compile("/resolve/realdebrid/[^/]+/([0-9a-fA-F]{40})(?:/|$)").matcher(url)
        return if (match.find()) match.group(1)?.lowercase() else null
    }
}
