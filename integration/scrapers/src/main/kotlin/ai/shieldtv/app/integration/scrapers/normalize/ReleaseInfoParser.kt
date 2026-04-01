package ai.shieldtv.app.integration.scrapers.normalize

import ai.shieldtv.app.core.model.source.Quality

data class ParsedReleaseInfo(
    val quality: Quality,
    val flags: Set<String>,
    val probablyPack: Boolean
)

object ReleaseInfoParser {
    fun parse(name: String): ParsedReleaseInfo {
        val lower = name.lowercase()
        val quality = when {
            "2160" in lower || "4k" in lower -> Quality.UHD_4K
            "1080" in lower -> Quality.FHD_1080P
            "720" in lower -> Quality.HD_720P
            "cam" in lower || "hdcam" in lower -> Quality.CAM
            else -> Quality.SD
        }
        val flags = buildSet {
            if ("hdr" in lower) add("hdr")
            if ("dv" in lower || "dolby vision" in lower) add("dv")
            if ("remux" in lower) add("remux")
            if ("bluray" in lower || "bdrip" in lower || "brrip" in lower) add("bluray")
            if ("web" in lower || "webrip" in lower || "web-dl" in lower) add("web")
            if ("hevc" in lower || "x265" in lower) add("hevc")
            if ("av1" in lower) add("av1")
            if ("atmos" in lower) add("atmos")
        }
        val probablyPack = listOf("complete", "collection", "pack", "season", "episodes", "series", "trilogy").any { it in lower }
        return ParsedReleaseInfo(quality = quality, flags = flags, probablyPack = probablyPack)
    }
}
