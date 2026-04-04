package ai.shieldtv.app.settings

class SourcePreferencesCoordinator(
    private val sourcePreferencesStore: SourcePreferencesStore,
    private val availableProviderIds: () -> Set<String>
) {
    fun currentPreferences(): SourcePreferences = sourcePreferencesStore.load()

    fun buildProviderSelectionLabel(): String {
        val prefs = currentPreferences()
        val allProviders = availableProviderIds()
        val effectiveProviders = prefs.providerSelection.effectiveEnabledProviders(allProviders)
        return when (prefs.providerSelection.mode) {
            ProviderSelectionMode.ALL_ENABLED -> "All enabled"
            ProviderSelectionMode.CUSTOM -> "${effectiveProviders.size} selected"
        }
    }

    fun setMovieMaxSizeGb(value: Int?) {
        sourcePreferencesStore.saveMovieMaxSizeGb(value)
    }

    fun setEpisodeMaxSizeGb(value: Int?) {
        sourcePreferencesStore.saveEpisodeMaxSizeGb(value)
    }

    fun toggleProvider(providerId: String) {
        val allProviders = availableProviderIds()
        val current = currentPreferences().providerSelection
        val working = current.effectiveEnabledProviders(allProviders).toMutableSet()
        if (!working.add(providerId)) {
            working.remove(providerId)
        }
        sourcePreferencesStore.saveProviderSelection(
            if (working == allProviders) {
                ProviderSelectionState(mode = ProviderSelectionMode.ALL_ENABLED)
            } else {
                ProviderSelectionState(
                    mode = ProviderSelectionMode.CUSTOM,
                    enabledProviders = working
                )
            }
        )
    }

    fun enableAllProviders() {
        sourcePreferencesStore.saveProviderSelection(
            ProviderSelectionState(mode = ProviderSelectionMode.ALL_ENABLED)
        )
    }

    fun disableAllProviders() {
        sourcePreferencesStore.saveProviderSelection(
            ProviderSelectionState(
                mode = ProviderSelectionMode.CUSTOM,
                enabledProviders = emptySet()
            )
        )
    }

    fun reset() {
        sourcePreferencesStore.reset()
    }
}
