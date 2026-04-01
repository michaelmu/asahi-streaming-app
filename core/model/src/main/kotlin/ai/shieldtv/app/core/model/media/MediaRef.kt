package ai.shieldtv.app.core.model.media

data class MediaRef(
    val mediaType: MediaType,
    val ids: MediaIds,
    val title: String,
    val originalTitle: String? = null,
    val year: Int? = null
)
