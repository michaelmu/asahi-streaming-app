package ai.shieldtv.app.integration.scrapers.provider.bitmagnet

import ai.shieldtv.app.domain.provider.RawProviderSource
import ai.shieldtv.app.integration.scrapers.normalize.ReleaseInfoParser
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderParser
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class BitmagnetParser : ProviderParser {
    override fun parse(rawResponse: String): List<RawProviderSource> {
        if (rawResponse.isBlank()) return emptyList()
        return runCatching {
            val builder = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(rawResponse.toByteArray(Charsets.UTF_8)))
            val items = doc.getElementsByTagName("item")
            val results = mutableListOf<RawProviderSource>()
            for (index in 0 until items.length) {
                val item = items.item(index) as? Element ?: continue
                val title = item.getElementsByTagName("title").item(0)?.textContent?.trim().orEmpty()
                if (title.isBlank()) continue
                val parsedRelease = ReleaseInfoParser.parse(title)
                if (parsedRelease.probablyPack) continue
                val attrs = item.getElementsByTagNameNS("http://torznab.com/schemas/2015/feed", "attr")
                var infoHash: String? = null
                var sizeBytes: Long? = null
                var seeders: Int? = null
                for (attrIndex in 0 until attrs.length) {
                    val attr = attrs.item(attrIndex) as? Element ?: continue
                    when (attr.getAttribute("name")) {
                        "infohash" -> infoHash = attr.getAttribute("value").ifBlank { null }
                        "size" -> sizeBytes = attr.getAttribute("value").toLongOrNull()
                        "seeders" -> seeders = attr.getAttribute("value").toIntOrNull()
                    }
                }
                val normalizedHash = infoHash?.lowercase() ?: continue
                results += RawProviderSource(
                    providerId = "bitmagnet",
                    title = title,
                    url = "magnet:?xt=urn:btih:$normalizedHash&dn=$title",
                    infoHash = normalizedHash,
                    sizeBytes = sizeBytes,
                    seeders = seeders,
                    extra = buildMap {
                        put("transport", "bitmagnet")
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
