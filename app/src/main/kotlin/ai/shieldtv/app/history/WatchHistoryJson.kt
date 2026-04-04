package ai.shieldtv.app.history

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaType
import org.json.JSONArray
import org.json.JSONObject

internal object WatchHistoryJson {
    private const val VERSION = 1

    fun encode(items: List<WatchHistoryItem>): String {
        val payload = JSONObject()
            .put("version", VERSION)
            .put(
                "items",
                JSONArray().apply {
                    items.forEach { item ->
                        put(
                            JSONObject()
                                .put("mediaType", item.mediaType.name)
                                .put("tmdbId", item.ids.tmdbId)
                                .put("imdbId", item.ids.imdbId)
                                .put("tvdbId", item.ids.tvdbId)
                                .put("title", item.title)
                                .put("year", item.year)
                                .put("artworkUrl", item.artworkUrl)
                                .put("subtitle", item.subtitle)
                                .put("seasonNumber", item.seasonNumber)
                                .put("episodeNumber", item.episodeNumber)
                                .put("episodeTitle", item.episodeTitle)
                                .put("watchedAtEpochMs", item.watchedAtEpochMs)
                        )
                    }
                }
            )
        return payload.toString()
    }

    fun decode(raw: String): List<WatchHistoryItem> {
        return runCatching {
            val root = JSONObject(raw)
            val items = root.optJSONArray("items") ?: JSONArray()
            buildList {
                for (index in 0 until items.length()) {
                    val json = items.optJSONObject(index) ?: continue
                    val mediaType = json.optString("mediaType")
                        .takeIf { it.isNotBlank() }
                        ?.let { MediaType.valueOf(it) }
                        ?: continue
                    val title = json.optString("title")
                    if (title.isBlank()) continue
                    add(
                        WatchHistoryItem(
                            mediaType = mediaType,
                            ids = MediaIds(
                                tmdbId = json.optString("tmdbId").ifBlank { null },
                                imdbId = json.optString("imdbId").ifBlank { null },
                                tvdbId = json.optString("tvdbId").ifBlank { null }
                            ),
                            title = title,
                            year = json.optInt("year").takeIf { json.has("year") },
                            artworkUrl = json.optString("artworkUrl").ifBlank { null },
                            subtitle = json.optString("subtitle").ifBlank { null },
                            seasonNumber = json.optInt("seasonNumber").takeIf { json.has("seasonNumber") },
                            episodeNumber = json.optInt("episodeNumber").takeIf { json.has("episodeNumber") },
                            episodeTitle = json.optString("episodeTitle").ifBlank { null },
                            watchedAtEpochMs = json.optLong("watchedAtEpochMs")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
