package ai.shieldtv.app.settings

import ai.shieldtv.app.ui.InfoModalSpec
import ai.shieldtv.app.ui.ModalDefaultAction

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
