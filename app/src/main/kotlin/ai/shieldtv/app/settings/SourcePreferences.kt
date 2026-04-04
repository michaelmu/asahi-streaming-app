package ai.shieldtv.app.settings

data class SourcePreferences(
    val movieMaxSizeGb: Int? = null,
    val episodeMaxSizeGb: Int? = null,
    val providerSelection: ProviderSelectionState = ProviderSelectionState()
) {
    val enabledProviders: Set<String>
        get() = providerSelection.enabledProviders
}
