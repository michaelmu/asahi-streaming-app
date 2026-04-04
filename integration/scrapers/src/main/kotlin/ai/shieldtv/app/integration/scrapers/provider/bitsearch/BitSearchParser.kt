package ai.shieldtv.app.integration.scrapers.provider.bitsearch

import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.integration.scrapers.normalize.ReleaseInfoParser
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderParser
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.regex.Pattern

class BitSearchParser : ProviderParser {
    override fun parse(rawResponse: String): List<RawProviderSource> {
        if (rawResponse.isBlank()) return emptyList()
        return runCatching {
            val doc = Jsoup.parse(rawResponse)
            val downloadLinks = doc.select("a[href^=/download/torrent/]")
            downloadLinks.mapNotNull { downloadLink ->
                val container = downloadLink.parent()?.parent() ?: downloadLink.parent() ?: return@mapNotNull null
                val magnet = container.select("a[href^=magnet:]").lastOrNull()?.attr("href")?.ifBlank { null }
                    ?: downloadLink.parent()?.select("a[href^=magnet:]")?.lastOrNull()?.attr("href")?.ifBlank { null }
                    ?: return@mapNotNull null
                val normalizedMagnet = URLDecoder.decode(magnet, "UTF-8")
                    .replace("&amp;", "&")
                    .replace("&#x3D;", "=")
                    .replace(" ", ".")
                val infoHash = extractInfoHash(normalizedMagnet) ?: return@mapNotNull null
                val name = extractDn(normalizedMagnet)
                    ?.removePrefix("[Bitsearch.to] ")
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val parsedRelease = ReleaseInfoParser.parse(name)
                val textBlob = container.text()
                val sizeBytes = extractSizeBytes(textBlob)
                val seeders = extractSeeders(textBlob)
                RawProviderSource(
                    providerId = "bitsearch",
                    title = name,
                    url = normalizedMagnet,
                    infoHash = infoHash,
                    sizeBytes = sizeBytes,
                    seeders = seeders,
                    extra = buildMap {
                        put("transport", "bitsearch")
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

    private fun extractSeeders(text: String): Int? {
        val match = Pattern.compile("([0-9]+(?:\\.[0-9]+)?K?)\\s+seeders", Pattern.CASE_INSENSITIVE).matcher(text)
        val normalized = if (match.find()) match.group(1) else null
        val value = normalized?.replace(",", "")?.trim() ?: return null
        return when {
            value.endsWith("K", ignoreCase = true) -> value.dropLast(1).toDoubleOrNull()?.times(1000)?.toInt()
            else -> value.toIntOrNull()
        }
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
