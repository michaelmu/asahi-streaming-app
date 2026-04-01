package ai.shieldtv.app.integration.metadata.tmdb.mapper

import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.TitleDetails

class TmdbDetailsMapper {
    fun fromMediaRef(mediaRef: MediaRef): TitleDetails {
        return TitleDetails(
            mediaRef = mediaRef,
            overview = "Placeholder details for ${mediaRef.title}",
            posterUrl = null,
            backdropUrl = null,
            genres = emptyList(),
            runtimeMinutes = null
        )
    }
}
