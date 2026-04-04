package ai.shieldtv.app.errors

sealed class PlaybackPrepareError(
    open val detail: String?
) {
    data class AuthRequired(override val detail: String? = null) : PlaybackPrepareError(detail)
    data class ResolveFailed(override val detail: String? = null) : PlaybackPrepareError(detail)
    data class PrepareFailed(override val detail: String? = null) : PlaybackPrepareError(detail)
}

sealed class UpdateFlowError(
    open val detail: String?
) {
    data class CheckFailed(override val detail: String? = null) : UpdateFlowError(detail)
    data class DownloadFailed(override val detail: String? = null) : UpdateFlowError(detail)
    data class InstallUnavailable(override val detail: String? = null) : UpdateFlowError(detail)
}

sealed class AuthFlowError(
    open val detail: String?
) {
    data class StartFailed(override val detail: String? = null) : AuthFlowError(detail)
    data class PollFailed(override val detail: String? = null) : AuthFlowError(detail)
    data class TimedOut(override val detail: String? = null) : AuthFlowError(detail)
}
