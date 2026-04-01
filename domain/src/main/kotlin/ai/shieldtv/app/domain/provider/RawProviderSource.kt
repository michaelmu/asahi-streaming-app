package ai.shieldtv.app.domain.provider

data class RawProviderSource(
    val providerId: String,
    val title: String,
    val url: String,
    val infoHash: String? = null,
    val sizeBytes: Long? = null,
    val seeders: Int? = null,
    val extra: Map<String, String> = emptyMap()
)
