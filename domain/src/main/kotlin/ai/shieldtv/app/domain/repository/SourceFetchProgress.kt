package ai.shieldtv.app.domain.repository

data class SourceFetchProgress(
    val providerId: String,
    val providerDisplayName: String,
    val state: State,
    val resultCount: Int? = null,
    val message: String? = null
) {
    enum class State {
        STARTED,
        COMPLETED,
        FAILED
    }
}
