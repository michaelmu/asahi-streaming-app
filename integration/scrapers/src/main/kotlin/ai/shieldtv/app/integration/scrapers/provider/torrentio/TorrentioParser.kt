package ai.shieldtv.app.integration.scrapers.provider.torrentio

import ai.shieldtv.app.domain.provider.RawProviderSource
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
                val infoHash = item.optString("infoHash").ifBlank { null }
                val url = item.optString("url").ifBlank {
                    infoHash?.let { "magnet:?xt=urn:btih:$it&dn=${primaryTitle.ifBlank { "torrentio" }}" } ?: ""
                }
                if (primaryTitle.isBlank() || url.isBlank()) continue

                val sizeBytes = extractSizeBytes(titleBlock)
                val seeders = extractSeeders(titleBlock)
                val qualityHint = when {
                    primaryTitle.contains("2160", ignoreCase = true) || primaryTitle.contains("4k", ignoreCase = true) -> "4k"
                    primaryTitle.contains("1080", ignoreCase = true) -> "1080p"
                    primaryTitle.contains("720", ignoreCase = true) -> "720p"
                    else -> "sd"
                }

                mapped += RawProviderSource(
                    providerId = "torrentio",
                    title = primaryTitle,
                    url = url,
                    infoHash = infoHash,
                    sizeBytes = sizeBytes,
                    seeders = seeders,
                    extra = buildMap {
                        put("quality_hint", qualityHint)
                        put("transport", "torrentio")
                    }
                )
            }
            mapped
        }.getOrElse { emptyList() }
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
}
