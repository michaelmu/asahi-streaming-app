package ai.shieldtv.app.settings

data class SourcePreferences(
    val movieMaxSizeGb: Int? = null,
    val episodeMaxSizeGb: Int? = null,
    val enabledProviders: Set<String> = emptySet()
)
