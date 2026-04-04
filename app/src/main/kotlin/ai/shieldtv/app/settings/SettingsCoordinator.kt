package ai.shieldtv.app.settings

import ai.shieldtv.app.auth.RealDebridAuthCoordinator
import ai.shieldtv.app.auth.RealDebridLinkStartResult
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.update.UpdateCheckUiResult
import ai.shieldtv.app.update.UpdateUiCoordinator

class SettingsCoordinator(
    private val sourcePreferencesCoordinator: SourcePreferencesCoordinator,
    private val availableProviderLabels: () -> Map<String, String>,
    private val realDebridAuthCoordinator: RealDebridAuthCoordinator,
    private val updateUiCoordinator: UpdateUiCoordinator
) {
    fun buildProviderSummary(): String {
        val labels = availableProviderLabels()
        val allProviders = labels.keys.toSet()
        val prefs = sourcePreferencesCoordinator.currentPreferences()
        val enabled = prefs.providerSelection.effectiveEnabledProviders(allProviders)
        val modeLabel = when (prefs.providerSelection.mode) {
            ProviderSelectionMode.ALL_ENABLED -> "all enabled"
            ProviderSelectionMode.CUSTOM -> "custom"
        }
        return labels.entries.joinToString(" • ") { (id, label) ->
            val marker = if (id in enabled) "on" else "off"
            "$label:$marker"
        } + " ($modeLabel)"
    }

    fun buildSourcePreferencesSummary(): String {
        val prefs = sourcePreferencesCoordinator.currentPreferences()
        val allProviders = availableProviderLabels().keys.toSet()
        val effectiveProviders = prefs.providerSelection.effectiveEnabledProviders(allProviders)
        val providerMode = when (prefs.providerSelection.mode) {
            ProviderSelectionMode.ALL_ENABLED -> "Providers: all enabled"
            ProviderSelectionMode.CUSTOM -> "Providers: ${effectiveProviders.joinToString(", ")}"
        }
        val movieLimit = "Movies max: ${prefs.movieMaxSizeGb?.let { "${it}GB" } ?: "none"}"
        val tvLimit = "TV max: ${prefs.episodeMaxSizeGb?.let { "${it}GB" } ?: "none"}"
        return listOf(providerMode, movieLimit, tvLimit).joinToString(" • ")
    }

    fun favoritesStatusLabel(mediaType: MediaType): String {
        return if (mediaType == MediaType.SHOW) "TV favorites" else "Movie favorites"
    }

    fun historyStatusLabel(mediaType: MediaType): String {
        return if (mediaType == MediaType.SHOW) "TV watch history" else "Movie watch history"
    }

    fun resetAuth(): RealDebridAuthState = realDebridAuthCoordinator.resetAuth()

    suspend fun startLink(): RealDebridLinkStartResult = realDebridAuthCoordinator.startLink()

    suspend fun checkForUpdates(): UpdateCheckUiResult = updateUiCoordinator.checkForUpdates()
}
