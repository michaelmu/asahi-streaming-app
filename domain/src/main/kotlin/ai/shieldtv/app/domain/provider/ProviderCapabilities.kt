package ai.shieldtv.app.domain.provider

data class ProviderCapabilities(
    val supportsMovies: Boolean = true,
    val supportsEpisodes: Boolean = true,
    val supportsSeasonPacks: Boolean = false,
    val supportsSeriesPacks: Boolean = false,
    val requiresRealDebrid: Boolean = false,
    val returnsMagnets: Boolean = true,
    val returnsResolvedLinks: Boolean = false,
    val productionReady: Boolean = true
)
