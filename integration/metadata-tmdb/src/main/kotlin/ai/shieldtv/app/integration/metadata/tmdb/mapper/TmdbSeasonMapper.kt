package ai.shieldtv.app.integration.metadata.tmdb.mapper

import ai.shieldtv.app.core.model.media.EpisodeSummary
import org.json.JSONObject

class TmdbSeasonMapper {
    fun fromJson(json: String): List<EpisodeSummary> {
        return runCatching {
            val root = JSONObject(json)
            val seasonNumber = root.optInt("season_number")
            val episodes = root.optJSONArray("episodes")
            buildList {
                for (index in 0 until episodes.length()) {
                    val item = episodes.optJSONObject(index) ?: continue
                    val episodeNumber = item.optInt("episode_number")
                    if (episodeNumber <= 0) continue
                    add(
                        EpisodeSummary(
                            seasonNumber = item.optInt("season_number").takeIf { it > 0 } ?: seasonNumber,
                            episodeNumber = episodeNumber,
                            title = item.optString("name").ifBlank { null },
                            overview = item.optString("overview").ifBlank { null },
                            thumbnailUrl = item.optString("still_path")
                                .takeIf { it.isNotBlank() }
                                ?.let { "https://image.tmdb.org/t/p/w500$it" },
                            airDate = item.optString("air_date").ifBlank { null }
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }
}
