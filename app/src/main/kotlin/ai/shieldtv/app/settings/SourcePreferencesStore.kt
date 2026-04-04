package ai.shieldtv.app.settings

import android.content.Context

class SourcePreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("source_preferences", Context.MODE_PRIVATE)

    fun load(): SourcePreferences {
        val movieMax = prefs.takeIf { it.contains(KEY_MOVIE_MAX_GB) }?.getInt(KEY_MOVIE_MAX_GB, 0)?.takeIf { it > 0 }
        val episodeMax = prefs.takeIf { it.contains(KEY_EPISODE_MAX_GB) }?.getInt(KEY_EPISODE_MAX_GB, 0)?.takeIf { it > 0 }

        val explicitMode = prefs.getString(KEY_PROVIDER_SELECTION_MODE, null)
        val storedEnabled = prefs.getStringSet(KEY_ENABLED_PROVIDERS, emptySet())?.toSet().orEmpty()
        val selectionState = when (explicitMode) {
            ProviderSelectionMode.CUSTOM.name -> ProviderSelectionState(
                mode = ProviderSelectionMode.CUSTOM,
                enabledProviders = storedEnabled
            )
            ProviderSelectionMode.ALL_ENABLED.name -> ProviderSelectionState(
                mode = ProviderSelectionMode.ALL_ENABLED,
                enabledProviders = emptySet()
            )
            else -> {
                if (storedEnabled.isEmpty()) {
                    ProviderSelectionState(mode = ProviderSelectionMode.ALL_ENABLED)
                } else {
                    ProviderSelectionState(
                        mode = ProviderSelectionMode.CUSTOM,
                        enabledProviders = storedEnabled
                    )
                }
            }
        }

        return SourcePreferences(
            movieMaxSizeGb = movieMax,
            episodeMaxSizeGb = episodeMax,
            providerSelection = selectionState
        )
    }

    fun saveMovieMaxSizeGb(value: Int?) {
        prefs.edit().apply {
            if (value == null) remove(KEY_MOVIE_MAX_GB) else putInt(KEY_MOVIE_MAX_GB, value)
        }.apply()
    }

    fun saveEpisodeMaxSizeGb(value: Int?) {
        prefs.edit().apply {
            if (value == null) remove(KEY_EPISODE_MAX_GB) else putInt(KEY_EPISODE_MAX_GB, value)
        }.apply()
    }

    fun saveProviderSelection(state: ProviderSelectionState) {
        prefs.edit()
            .putString(KEY_PROVIDER_SELECTION_MODE, state.mode.name)
            .putStringSet(KEY_ENABLED_PROVIDERS, state.enabledProviders)
            .apply()
    }

    fun saveEnabledProviders(value: Set<String>) {
        saveProviderSelection(
            if (value.isEmpty()) {
                ProviderSelectionState(mode = ProviderSelectionMode.ALL_ENABLED)
            } else {
                ProviderSelectionState(
                    mode = ProviderSelectionMode.CUSTOM,
                    enabledProviders = value
                )
            }
        )
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_MOVIE_MAX_GB = "movie_max_size_gb"
        private const val KEY_EPISODE_MAX_GB = "episode_max_size_gb"
        private const val KEY_ENABLED_PROVIDERS = "enabled_providers"
        private const val KEY_PROVIDER_SELECTION_MODE = "provider_selection_mode"
    }
}
