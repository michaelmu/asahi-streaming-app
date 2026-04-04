package ai.shieldtv.app.settings

data class ProviderSelectionState(
    val mode: ProviderSelectionMode = ProviderSelectionMode.ALL_ENABLED,
    val enabledProviders: Set<String> = emptySet()
) {
    fun effectiveEnabledProviders(allProviders: Set<String>): Set<String> {
        return when (mode) {
            ProviderSelectionMode.ALL_ENABLED -> allProviders
            ProviderSelectionMode.CUSTOM -> enabledProviders.intersect(allProviders)
        }
    }
}
