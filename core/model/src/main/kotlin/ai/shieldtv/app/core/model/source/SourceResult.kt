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
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val infoHash: String? = null,
    val sizeBytes: Long? = null,
    val sizeLabel: String? = null,
    val score: Double? = null,
    val providerIds: Set<String> = setOf(providerId),
    val providerDisplayNames: Set<String> = setOf(providerDisplayName),
    val rawMetadata: Map<String, String> = emptyMap(),
    val origins: List<SourceOrigin> = listOf(
        SourceOrigin(
            providerId = providerId,
            providerDisplayName = providerDisplayName,
            displayName = displayName,
            cacheStatus = cacheStatus,
            sizeBytes = sizeBytes,
            seeders = rawMetadata["seeders"]?.toIntOrNull(),
            quality = quality
        )
    )
)
