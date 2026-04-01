package ai.shieldtv.app.feature.player.ui

data class PlayerUiState(
    val loading: Boolean = false,
    val prepared: Boolean = false,
    val playbackUrl: String? = null,
    val error: String? = null
)
