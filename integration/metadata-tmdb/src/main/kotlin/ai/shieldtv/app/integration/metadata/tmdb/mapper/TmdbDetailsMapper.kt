package ai.shieldtv.app.integration.metadata.tmdb.mapper

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.TitleDetails

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
}
