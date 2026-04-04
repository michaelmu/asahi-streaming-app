package ai.shieldtv.app.domain.repository

sealed class SourceFetchError(
    val message: String?
) {
    class Timeout(message: String? = null) : SourceFetchError(message)
    class ProviderFailure(message: String? = null) : SourceFetchError(message)
}
