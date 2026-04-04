package ai.shieldtv.app.favorites

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaType
import org.json.JSONArray
import org.json.JSONObject

internal object FavoritesJson {
    private const val VERSION = 1

    fun encode(items: List<FavoriteItem>): String {
        return JSONObject()
            .put("version", VERSION)
            .put("items", JSONArray().apply {
                items.forEach { item ->
                    put(
                        JSONObject()
                            .put("mediaType", item.mediaType.name)
                            .put("tmdbId", item.ids.tmdbId)
                            .put("imdbId", item.ids.imdbId)
                            .put("tvdbId", item.ids.tvdbId)
                            .put("title", item.title)
                            .put("year", item.year)
                            .put("subtitle", item.subtitle)
                            .put("artworkUrl", item.artworkUrl)
                            .put("addedAtEpochMs", item.addedAtEpochMs)
                    )
                }
            })
            .toString()
    }

    fun decode(raw: String): List<FavoriteItem> {
        return runCatching {
            val json = JSONObject(raw)
            val items = json.optJSONArray("items") ?: JSONArray()
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    add(
                        FavoriteItem(
                            mediaType = MediaType.valueOf(item.optString("mediaType")),
                            ids = MediaIds(
                                tmdbId = item.optString("tmdbId").ifBlank { null },
                                imdbId = item.optString("imdbId").ifBlank { null },
                                tvdbId = item.optString("tvdbId").ifBlank { null }
                            ),
                            title = item.optString("title"),
                            year = item.optInt("year").takeIf { item.has("year") },
                            subtitle = item.optString("subtitle").ifBlank { null },
                            artworkUrl = item.optString("artworkUrl").ifBlank { null },
                            addedAtEpochMs = item.optLong("addedAtEpochMs")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
