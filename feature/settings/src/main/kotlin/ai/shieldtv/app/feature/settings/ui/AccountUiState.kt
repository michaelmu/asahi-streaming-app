package ai.shieldtv.app.feature.settings.ui

import ai.shieldtv.app.core.model.auth.RealDebridAuthState

data class AccountUiState(
    val loading: Boolean = false,
    val authState: RealDebridAuthState = RealDebridAuthState(isLinked = false),
    val error: String? = null
)
