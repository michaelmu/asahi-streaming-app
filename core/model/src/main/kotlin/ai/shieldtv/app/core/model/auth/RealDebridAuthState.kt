package ai.shieldtv.app.core.model.auth

data class RealDebridAuthState(
    val isLinked: Boolean,
    val username: String? = null,
    val authInProgress: Boolean = false,
    val lastError: String? = null
)
