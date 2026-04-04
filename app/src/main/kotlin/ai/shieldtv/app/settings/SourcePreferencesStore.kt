package ai.shieldtv.app.settings

import android.content.Context

class SourcePreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("source_preferences", Context.MODE_PRIVATE)

    fun load(): SourcePreferences {
        val movieMax = prefs.takeIf { it.contains(KEY_MOVIE_MAX_GB) }?.getInt(KEY_MOVIE_MAX_GB, 0)?.takeIf { it > 0 }
        val episodeMax = prefs.takeIf { it.contains(KEY_EPISODE_MAX_GB) }?.getInt(KEY_EPISODE_MAX_GB, 0)?.takeIf { it > 0 }
        val enabled = prefs.getStringSet(KEY_ENABLED_PROVIDERS, emptySet())?.toSet().orEmpty()
        return SourcePreferences(
            movieMaxSizeGb = movieMax,
            episodeMaxSizeGb = episodeMax,
            enabledProviders = enabled
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

    fun saveEnabledProviders(value: Set<String>) {
        prefs.edit().putStringSet(KEY_ENABLED_PROVIDERS, value).apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_MOVIE_MAX_GB = "movie_max_size_gb"
        private const val KEY_EPISODE_MAX_GB = "episode_max_size_gb"
        private const val KEY_ENABLED_PROVIDERS = "enabled_providers"
    }
}
