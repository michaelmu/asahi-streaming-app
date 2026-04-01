package ai.shieldtv.app.integration.metadata.tmdb.mapper

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.TitleDetails
import org.json.JSONObject

class TmdbDetailsMapper {
    fun fromMediaRef(mediaRef: MediaRef): TitleDetails {
        return TitleDetails(
            mediaRef = mediaRef,
            overview = "Placeholder details for ${mediaRef.title}. This is where TMDb-backed metadata, artwork, and episode data will eventually land.",
            posterUrl = null,
            backdropUrl = null,
            genres = listOf("Drama", "Sci-Fi"),
            runtimeMinutes = if (mediaRef.mediaType.name == "MOVIE") 120 else 45
        )
    }

    fun fromJson(json: String, fallback: MediaRef): TitleDetails {
        return runCatching {
            val root = JSONObject(json)
            val genres = root.optJSONArray("genres")
            val genreNames = buildList {
                for (index in 0 until genres.length()) {
                    val item = genres.optJSONObject(index) ?: continue
                    val name = item.optString("name")
                    if (name.isNotBlank()) add(name)
                }
            }
            TitleDetails(
                mediaRef = fallback.copy(
                    title = root.optString("title").ifBlank { root.optString("name").ifBlank { fallback.title } },
                    year = root.optString("release_date").takeIf { it.isNotBlank() }?.take(4)?.toIntOrNull()
                        ?: root.optString("first_air_date").takeIf { it.isNotBlank() }?.take(4)?.toIntOrNull()
                        ?: fallback.year
                ),
                overview = root.optString("overview").ifBlank { null },
                posterUrl = root.optString("poster_path").takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = root.optString("backdrop_path").takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w780$it" },
                genres = genreNames,
                runtimeMinutes = root.optInt("runtime").takeIf { it > 0 }
                    ?: root.optJSONArray("episode_run_time")?.optInt(0)?.takeIf { it > 0 }
            )
        }.getOrElse {
            fromMediaRef(fallback)
        }
    }
}
