package ai.shieldtv.app.ui

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow

object AuthPreviewState {
    @Volatile
    var activeFlow: DeviceCodeFlow? = null
}
