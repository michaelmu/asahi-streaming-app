package ai.shieldtv.app.core.model.source

data class SourceOrigin(
    val providerId: String,
    val providerDisplayName: String,
    val displayName: String,
    val cacheStatus: CacheStatus,
    val sizeBytes: Long? = null,
    val seeders: Int? = null,
    val quality: Quality? = null
)
