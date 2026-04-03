package ai.shieldtv.app.integration.scrapers.provider.knaben

import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.integration.scrapers.normalize.ReleaseInfoParser
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderParser
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class KnabenParser : ProviderParser {
    override fun parse(rawResponse: String): List<RawProviderSource> {
        if (rawResponse.isBlank()) return emptyList()
        return runCatching {
            val doc = Jsoup.parse(rawResponse)
            val rows = doc.select("tr")
            rows.mapNotNull { row ->
                val magnet = row.select("a[href^=magnet:]").firstOrNull()?.attr("href")?.ifBlank { null } ?: return@mapNotNull null
                val normalizedMagnet = URLDecoder.decode(magnet, StandardCharsets.UTF_8)
                    .replace("&amp;", "&")
                    .replace("&#x3D;", "=")
                val infoHash = extractInfoHash(normalizedMagnet) ?: return@mapNotNull null
                val name = extractDn(normalizedMagnet)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val parsedRelease = ReleaseInfoParser.parse(name)
                val cells = row.select("td")
                val sizeBytes = extractSizeBytes(cells.getOrNull(2)?.text().orEmpty())
                val seeders = cells.getOrNull(4)?.text()?.replace(",", "")?.trim()?.toIntOrNull()
                RawProviderSource(
                    providerId = "knaben",
                    title = name,
                    url = normalizedMagnet,
                    infoHash = infoHash,
                    sizeBytes = sizeBytes,
                    seeders = seeders,
                    extra = buildMap {
                        put("transport", "knaben")
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
            }.distinctBy { it.infoHash }
        }.getOrElse {
            emptyList()
        }
    }

    private fun extractInfoHash(magnet: String): String? {
        val match = Pattern.compile("btih:([^&]+)", Pattern.CASE_INSENSITIVE).matcher(magnet)
        return if (match.find()) match.group(1)?.lowercase() else null
    }

    private fun extractDn(magnet: String): String? {
        val match = Pattern.compile("[?&]dn=([^&]+)", Pattern.CASE_INSENSITIVE).matcher(magnet)
        return if (match.find()) match.group(1) else null
    }

    private fun extractSizeBytes(text: String): Long? {
        val match = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(GB|MB|TB)", Pattern.CASE_INSENSITIVE).matcher(text)
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
