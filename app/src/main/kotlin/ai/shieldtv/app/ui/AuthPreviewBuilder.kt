package ai.shieldtv.app.ui

import ai.shieldtv.app.feature.SettingsFeatureFactory
import ai.shieldtv.app.integration.debrid.realdebrid.config.RealDebridConfig
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState

class AuthPreviewBuilder {
    suspend fun build(): String {
        val settingsViewModel = SettingsFeatureFactory.createViewModel()

        val existingFlow = AuthPreviewState.activeFlow
        val startedState = if (existingFlow == null) {
            settingsViewModel.startLinking().also { state ->
                AuthPreviewState.activeFlow = state.deviceCodeFlow
            }
        } else {
            null
        }

        val flow = existingFlow ?: startedState?.deviceCodeFlow
        val polledState = flow?.let { settingsViewModel.pollLinking(it) }

        if (polledState?.authState?.isLinked == true) {
            AuthPreviewState.activeFlow = null
        }

        return buildString {
            appendLine("Real-Debrid Auth Preview:")
            appendLine("API mode: ${RealDebridDebugState.lastApiMode}")
            appendLine("Bootstrap client id: ${RealDebridConfig.clientId()}")
            appendLine("Linked: ${polledState?.authState?.isLinked ?: startedState?.authState?.isLinked ?: false}")
            appendLine("Auth in progress: ${polledState?.authState?.authInProgress ?: startedState?.authState?.authInProgress ?: false}")
            appendLine("Verification URL: ${flow?.verificationUrl ?: "none"}")
            appendLine("User Code: ${flow?.userCode ?: "none"}")
            appendLine("Direct Verification URL: ${RealDebridDebugState.lastDirectVerificationUrl.ifBlank { flow?.qrCodeUrl ?: "none" }}")
            appendLine("Start response: ${RealDebridDebugState.lastStartDeviceFlowResponse.ifBlank { "none" }}")
            appendLine("Start error: ${RealDebridDebugState.lastStartDeviceFlowError.ifBlank { "none" }}")
            appendLine("Credentials response: ${RealDebridDebugState.lastCredentialsResponse.ifBlank { "none" }}")
            appendLine("Credentials error: ${RealDebridDebugState.lastCredentialsError.ifBlank { "none" }}")
            appendLine("Token response: ${RealDebridDebugState.lastTokenResponse.ifBlank { "none" }}")
            appendLine("Token error: ${RealDebridDebugState.lastTokenError.ifBlank { "none" }}")
            appendLine("Error: ${polledState?.error ?: startedState?.error ?: "none"}")
        }
    }
}
