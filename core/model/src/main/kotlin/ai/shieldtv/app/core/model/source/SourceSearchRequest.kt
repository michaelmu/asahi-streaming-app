package ai.shieldtv.app.core.model.source

import ai.shieldtv.app.core.model.media.MediaRef

data class SourceSearchRequest(
    val mediaRef: MediaRef,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val aliases: List<String> = emptyList(),
    val filters: SourceFilters = SourceFilters()
)
