package ai.shieldtv.app.core.model.source

import ai.shieldtv.app.core.model.media.MediaRef

data class SourceResult(
    val id: String,
    val mediaRef: MediaRef,
    val providerId: String,
    val providerDisplayName: String,
    val providerKind: ProviderKind,
    val debridService: DebridService,
    val sourceSite: String?,
    val url: String,
    val displayName: String,
    val quality: Quality,
    val cacheStatus: CacheStatus,
    val infoHash: String? = null,
    val sizeBytes: Long? = null,
    val sizeLabel: String? = null,
    val score: Double? = null,
    val rawMetadata: Map<String, String> = emptyMap()
)
