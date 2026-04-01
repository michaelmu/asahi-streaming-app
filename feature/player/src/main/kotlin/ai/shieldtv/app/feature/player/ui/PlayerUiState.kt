package ai.shieldtv.app.feature.player.ui

data class PlayerUiState(
    val loading: Boolean = false,
    val prepared: Boolean = false,
    val error: String? = null
)
