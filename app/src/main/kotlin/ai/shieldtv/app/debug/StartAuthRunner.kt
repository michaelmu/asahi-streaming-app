package ai.shieldtv.app.debug

import ai.shieldtv.app.feature.SettingsFeatureFactory
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState

object StartAuthRunner {
    @JvmStatic
    suspend fun main(args: Array<String>) {
        val settingsViewModel = SettingsFeatureFactory.createViewModel()
        val state = settingsViewModel.startLinking()
        val flow = state.deviceCodeFlow
        if (flow == null) {
            println("No device flow returned")
            return
        }
        AuthDebugStore.save(flow)
        println("Verification URL: ${flow.verificationUrl}")
        println("User Code: ${flow.userCode}")
        println("Direct Verification URL: ${RealDebridDebugState.lastDirectVerificationUrl.ifBlank { flow.qrCodeUrl ?: "none" }}")
    }
}
