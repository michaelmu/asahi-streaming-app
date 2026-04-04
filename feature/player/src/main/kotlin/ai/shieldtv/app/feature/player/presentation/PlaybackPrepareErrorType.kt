package ai.shieldtv.app.feature.player.presentation

sealed class PlaybackPrepareErrorType {
    data object ResolveFailed : PlaybackPrepareErrorType()
    data object PrepareFailed : PlaybackPrepareErrorType()
}
