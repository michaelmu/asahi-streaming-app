package ai.shieldtv.app.ui

import ai.shieldtv.app.feature.SettingsFeatureFactory
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.debrid.realdebrid.config.RealDebridConfig
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState

class AuthPreviewBuilder {
    suspend fun build(): String {
        val settingsViewModel = SettingsFeatureFactory.createViewModel()
        val tokenStore = RealDebridTokenStore()

        val currentState = settingsViewModel.getAuthState()
        val existingFlow = AuthPreviewState.activeFlow
        val startedState = if (!currentState.authState.isLinked && existingFlow == null) {
            settingsViewModel.startLinking().also { state ->
                AuthPreviewState.activeFlow = state.deviceCodeFlow
            }
        } else {
            null
        }

        val flow = existingFlow ?: startedState?.deviceCodeFlow
        val polledState = if (!currentState.authState.isLinked) flow?.let { settingsViewModel.pollLinking(it) } else null

        if (currentState.authState.isLinked || polledState?.authState?.isLinked == true) {
            AuthPreviewState.activeFlow = null
        }

        return buildString {
            appendLine("Real-Debrid Auth Preview:")
            appendLine("API mode: ${RealDebridDebugState.lastApiMode}")
            appendLine("Bootstrap client id: ${RealDebridConfig.clientId()}")
            appendLine("Linked: ${polledState?.authState?.isLinked ?: startedState?.authState?.isLinked ?: currentState.authState.isLinked}")
            appendLine("Auth in progress: ${polledState?.authState?.authInProgress ?: startedState?.authState?.authInProgress ?: currentState.authState.authInProgress}")
            appendLine("Verification URL: ${flow?.verificationUrl ?: "none"}")
            appendLine("User Code: ${flow?.userCode ?: "none"}")
            appendLine("Direct Verification URL: ${RealDebridDebugState.lastDirectVerificationUrl.ifBlank { flow?.qrCodeUrl ?: "none" }}")
            appendLine("Start response: ${RealDebridDebugState.lastStartDeviceFlowResponse.ifBlank { "none" }}")
            appendLine("Start error: ${RealDebridDebugState.lastStartDeviceFlowError.ifBlank { "none" }}")
            appendLine("Credentials response: ${RealDebridDebugState.lastCredentialsResponse.ifBlank { "none" }}")
            appendLine("Credentials error: ${RealDebridDebugState.lastCredentialsError.ifBlank { "none" }}")
            appendLine("Token response: ${RealDebridDebugState.lastTokenResponse.ifBlank { "none" }}")
            appendLine("Token error: ${RealDebridDebugState.lastTokenError.ifBlank { "none" }}")
            appendLine("Instant availability request: ${RealDebridDebugState.lastInstantAvailabilityRequest.ifBlank { "none" }}")
            appendLine("Instant availability response: ${RealDebridDebugState.lastInstantAvailabilityResponse.ifBlank { "none" }}")
            appendLine("Instant availability error: ${RealDebridDebugState.lastInstantAvailabilityError.ifBlank { "none" }}")
            appendLine("Cache marker hash count: ${RealDebridDebugState.lastCacheMarkerHashCount.ifBlank { "none" }}")
            appendLine("Cache marker cached count: ${RealDebridDebugState.lastCacheMarkerCachedCount.ifBlank { "none" }}")
            appendLine("Source repository seen: ${RealDebridDebugState.lastSourceRepositorySeen.ifBlank { "none" }}")
            appendLine("Source repository marker present: ${RealDebridDebugState.lastSourceRepositoryMarkerPresent.ifBlank { "none" }}")
            appendLine("Token store save called: ${RealDebridDebugState.lastTokenStoreSaveCalled.ifBlank { "none" }}")
            appendLine("Token store write path: ${RealDebridDebugState.lastTokenStoreWritePath.ifBlank { tokenStore.debugFilePath() }}")
            appendLine("Token store write exists: ${RealDebridDebugState.lastTokenStoreWriteExists.ifBlank { if (tokenStore.get() != null) "yes" else "no" }}")
            appendLine("Error: ${polledState?.error ?: startedState?.error ?: currentState.error ?: "none"}")
        }
    }
}
