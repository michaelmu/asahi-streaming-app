package ai.shieldtv.app.settings

import ai.shieldtv.app.auth.RealDebridLinkStartFailure
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.update.AppUpdateInfo
import ai.shieldtv.app.ui.InfoModalSpec
import ai.shieldtv.app.ui.ModalDefaultAction
import ai.shieldtv.app.update.UpdateInstallUiResult

class SettingsModalCoordinator(
    private val sourcePreferencesCoordinator: SourcePreferencesCoordinator,
    private val availableProviderLabels: () -> Map<String, String>
) {
    fun movieMaxSizeSpec(onSelected: (Int?) -> Unit): InfoModalSpec {
        val currentValue = sourcePreferencesCoordinator.currentPreferences().movieMaxSizeGb
        return sizePickerSpec(
            title = "Movie Max Size",
            currentValue = currentValue,
            values = listOf(null, 1, 2, 4, 8, 12, 16, 24, 32, 48),
            valueLabel = { it?.let { gb -> "${gb}GB" } ?: "No limit" },
            onSelected = onSelected
        )
    }

    fun tvMaxSizeSpec(onSelected: (Int?) -> Unit): InfoModalSpec {
        val currentValue = sourcePreferencesCoordinator.currentPreferences().episodeMaxSizeGb
        return sizePickerSpec(
            title = "TV Max Size",
            currentValue = currentValue,
            values = listOf(null, 1, 2, 4, 6, 8, 12, 16, 24),
            valueLabel = { it?.let { gb -> "${gb}GB" } ?: "No limit" },
            onSelected = onSelected
        )
    }

    fun providerSelectionSpec(
        startIndex: Int,
        onToggleProvider: (String) -> Unit,
        onEnableAll: () -> Unit,
        onDisableAll: () -> Unit,
        onNextPage: (Int) -> Unit,
        onOpenActions: (List<String>) -> Unit
    ): InfoModalSpec {
        val labels = availableProviderLabels()
        val allProviders = labels.keys.toList()
        val current = sourcePreferencesCoordinator.currentPreferences().providerSelection
        val effectiveEnabled = current.effectiveEnabledProviders(allProviders.toSet())
        val page = allProviders.drop(startIndex).take(3)
        val providerLines = allProviders.joinToString("\n") { id ->
            val enabled = id in effectiveEnabled
            val marker = if (enabled) "[x]" else "[ ]"
            "$marker ${labels[id] ?: id}"
        }
        return InfoModalSpec(
            title = "Choose Providers",
            message = buildString {
                appendLine("Enabled: ${effectiveEnabled.size} / ${allProviders.size}")
                appendLine()
                append(providerLines)
            },
            primaryLabel = page.getOrNull(0)?.let { id -> formatProviderChoiceLabel(labels[id] ?: id, id in effectiveEnabled) } ?: "Enable All",
            onPrimary = {
                val provider = page.getOrNull(0)
                if (provider != null) onToggleProvider(provider) else onEnableAll()
            },
            secondaryLabel = page.getOrNull(1)?.let { id -> formatProviderChoiceLabel(labels[id] ?: id, id in effectiveEnabled) } ?: "Disable All",
            onSecondary = {
                val provider = page.getOrNull(1)
                if (provider != null) onToggleProvider(provider) else onDisableAll()
            },
            tertiaryLabel = when {
                page.getOrNull(2) != null -> formatProviderChoiceLabel(labels[page[2]] ?: page[2], page[2] in effectiveEnabled)
                startIndex + 3 < allProviders.size -> "More…"
                else -> "Actions"
            },
            onTertiary = {
                val provider = page.getOrNull(2)
                when {
                    provider != null -> onToggleProvider(provider)
                    startIndex + 3 < allProviders.size -> onNextPage(startIndex + 3)
                    else -> onOpenActions(allProviders)
                }
            },
            dismissOnBack = true,
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    fun providerSelectionActionsSpec(
        allProviders: List<String>,
        onEnableAll: () -> Unit,
        onDisableAll: () -> Unit,
        onDone: () -> Unit
    ): InfoModalSpec {
        val labels = availableProviderLabels()
        val current = sourcePreferencesCoordinator.currentPreferences().providerSelection
        val effectiveEnabled = current.effectiveEnabledProviders(allProviders.toSet())
        val providerLines = allProviders.joinToString("\n") { id ->
            val enabled = id in effectiveEnabled
            val marker = if (enabled) "[x]" else "[ ]"
            "$marker ${labels[id] ?: id}"
        }
        return InfoModalSpec(
            title = "Provider Selection",
            message = providerLines,
            primaryLabel = "Enable All",
            onPrimary = onEnableAll,
            secondaryLabel = "Disable All",
            onSecondary = onDisableAll,
            tertiaryLabel = "Done",
            onTertiary = onDone,
            dismissOnBack = true,
            defaultAction = ModalDefaultAction.TERTIARY
        )
    }

    fun resetSourcePreferencesSpec(onConfirm: () -> Unit): InfoModalSpec {
        return InfoModalSpec(
            title = "Source Preferences Reset",
            message = "Movie/TV size limits and provider overrides were reset.",
            primaryLabel = "OK",
            onPrimary = onConfirm
        )
    }

    fun realDebridLinkFailedSpec(
        failure: RealDebridLinkStartFailure,
        onCopyDebugInfo: () -> Unit
    ): InfoModalSpec {
        return InfoModalSpec(
            title = "Real-Debrid Link Failed",
            message = "${failure.errorType}: ${failure.debugMessage}",
            primaryLabel = "OK",
            secondaryLabel = "Copy Debug Info",
            onSecondary = onCopyDebugInfo,
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    fun realDebridFlowSpec(
        flow: DeviceCodeFlow,
        authUrl: String,
        qrContent: android.view.View,
        onOpenLinkPage: () -> Unit,
        onCopyDebugInfo: () -> Unit
    ): InfoModalSpec {
        return InfoModalSpec(
            title = "Link Real-Debrid",
            message = buildString {
                appendLine("Open: $authUrl")
                appendLine()
                append("Code: ${flow.userCode}")
            },
            primaryLabel = "Open Link Page",
            onPrimary = onOpenLinkPage,
            secondaryLabel = "Copy Debug Info",
            onSecondary = onCopyDebugInfo,
            tertiaryLabel = "Close",
            onTertiary = {},
            defaultAction = ModalDefaultAction.PRIMARY,
            customContent = qrContent
        )
    }

    fun realDebridLinkedSpec(linkedMessage: String): InfoModalSpec {
        return InfoModalSpec(
            title = "Real-Debrid Linked",
            message = linkedMessage,
            primaryLabel = "Back to Settings",
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    fun realDebridTimedOutSpec(
        errorType: String,
        timeoutMessage: String,
        onTryAgain: () -> Unit
    ): InfoModalSpec {
        return InfoModalSpec(
            title = "Real-Debrid Link Timed Out",
            message = "$errorType: $timeoutMessage",
            primaryLabel = "Try Again",
            onPrimary = onTryAgain,
            secondaryLabel = "Close",
            onSecondary = {},
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    fun updateCheckFailedSpec(errorMessage: String): InfoModalSpec {
        return InfoModalSpec(
            title = "Update Check Failed",
            message = errorMessage,
            primaryLabel = "OK"
        )
    }

    fun updateAvailableSpec(
        updateInfo: AppUpdateInfo,
        onInstall: () -> Unit,
        onViewReleaseNotes: () -> Unit
    ): InfoModalSpec {
        return InfoModalSpec(
            title = "Update Available",
            message = buildString {
                append(updateInfo.versionName)
                updateInfo.versionCodeHint?.let {
                    append(" (versionCode ")
                    append(it)
                    append(")")
                }
                updateInfo.publishedAt?.takeIf { publishedAt -> publishedAt.isNotBlank() }?.let { publishedAt ->
                    appendLine()
                    appendLine()
                    append("Published: ")
                    append(publishedAt)
                }
            },
            primaryLabel = "Install Update",
            onPrimary = onInstall,
            secondaryLabel = "View Release Notes",
            onSecondary = onViewReleaseNotes,
            tertiaryLabel = "Later",
            onTertiary = {},
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    fun updateInstallSpec(
        result: UpdateInstallUiResult,
        onOpenSettings: (() -> Unit)? = null
    ): InfoModalSpec {
        return when (result) {
            is UpdateInstallUiResult.Ready -> InfoModalSpec(
                title = "Installer Launched",
                message = "If Android does not show the installer, check the unknown apps permission for Asahi.",
                primaryLabel = "Done",
                secondaryLabel = "Open Settings",
                onSecondary = onOpenSettings,
                defaultAction = ModalDefaultAction.PRIMARY
            )
            is UpdateInstallUiResult.RequiresSettings -> InfoModalSpec(
                title = "Enable APK Installs",
                message = result.readiness.message ?: "Android is blocking installs from Asahi right now. Allow installs from this app, then try again.",
                primaryLabel = "Open Settings",
                onPrimary = onOpenSettings,
                secondaryLabel = "Not Now",
                onSecondary = {},
                defaultAction = ModalDefaultAction.PRIMARY
            )
            is UpdateInstallUiResult.Unavailable -> InfoModalSpec(
                title = "Installer Unavailable",
                message = result.readiness.message ?: "No app on this device can handle APK installation intents right now.",
                primaryLabel = "OK"
            )
            is UpdateInstallUiResult.Failure -> InfoModalSpec(
                title = "Update Failed",
                message = result.errorMessage,
                primaryLabel = "OK"
            )
        }
    }

    private fun sizePickerSpec(
        title: String,
        currentValue: Int?,
        values: List<Int?>,
        valueLabel: (Int?) -> String,
        onSelected: (Int?) -> Unit
    ): InfoModalSpec {
        val currentIndex = values.indexOf(currentValue).takeIf { it >= 0 } ?: 0
        val pageStart = (currentIndex / 3) * 3
        val page = values.drop(pageStart).take(3)
        val lines = values.joinToString("\n") { value ->
            val marker = if (value == currentValue) "[x]" else "[ ]"
            "$marker ${valueLabel(value)}"
        }
        return InfoModalSpec(
            title = title,
            message = lines,
            primaryLabel = page.getOrNull(0)?.let(valueLabel) ?: "Close",
            onPrimary = { page.getOrNull(0)?.let(onSelected) },
            secondaryLabel = page.getOrNull(1)?.let(valueLabel),
            onSecondary = page.getOrNull(1)?.let { value -> { onSelected(value) } },
            tertiaryLabel = page.getOrNull(2)?.let(valueLabel),
            onTertiary = page.getOrNull(2)?.let { value -> { onSelected(value) } },
            dismissOnBack = true,
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    private fun formatProviderChoiceLabel(label: String, enabled: Boolean): String {
        return if (enabled) "Disable $label" else "Enable $label"
    }
}
