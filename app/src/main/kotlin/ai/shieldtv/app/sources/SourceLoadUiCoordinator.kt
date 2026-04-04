package ai.shieldtv.app.sources

import ai.shieldtv.app.AppCoordinator
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.ui.InfoModalSpec
import ai.shieldtv.app.ui.ModalDefaultAction

class SourceLoadUiCoordinator {
    fun buildSearchLabel(
        mediaRef: MediaRef,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): String {
        return if (seasonNumber != null && episodeNumber != null) {
            "${mediaRef.title} S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
        } else {
            mediaRef.title
        }
    }

    fun applyInitialShellState(
        coordinator: AppCoordinator,
        mediaRef: MediaRef,
        seasonNumber: Int?,
        episodeNumber: Int?
    ) {
        coordinator.showSources(
            mediaRef = mediaRef,
            details = coordinator.currentState().selectedDetails,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            sources = emptyList()
        )
    }

    fun applyIncrementalState(
        coordinator: AppCoordinator,
        mediaRef: MediaRef,
        seasonNumber: Int?,
        episodeNumber: Int?,
        sources: List<SourceResult>
    ) {
        coordinator.showSources(
            mediaRef = mediaRef,
            details = coordinator.currentState().selectedDetails,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            sources = sources
        )
    }

    fun progressSpec(
        searchLabel: String,
        progressItems: List<SourceFetchProgress>,
        onKeepWaiting: () -> Unit,
        onCancel: () -> Unit
    ): InfoModalSpec {
        val lines = if (progressItems.isEmpty()) {
            listOf("Preparing providers…")
        } else {
            progressItems.map { progress ->
                when (progress.state) {
                    SourceFetchProgress.State.STARTED -> "${progress.providerDisplayName}: querying…"
                    SourceFetchProgress.State.COMPLETED -> "${progress.providerDisplayName}: ${progress.resultCount ?: 0} result(s)"
                    SourceFetchProgress.State.FAILED -> "${progress.providerDisplayName}: failed${progress.message?.let { " (${it.take(60)})" } ?: ""}"
                }
            }
        }
        return InfoModalSpec(
            title = "Finding Sources",
            message = buildString {
                appendLine(searchLabel)
                appendLine()
                append(lines.joinToString("\n"))
            },
            primaryLabel = "Keep Waiting",
            onPrimary = onKeepWaiting,
            secondaryLabel = "Cancel",
            onSecondary = onCancel,
            dismissOnBack = false,
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    fun incrementalDiagnostics(
        sources: List<SourceResult>,
        completedProviders: Int,
        totalProviders: Int,
        providerSummary: String,
        buildDiagnostics: (List<SourceResult>) -> String
    ): String {
        return buildDiagnostics(sources) +
            " | progress=${completedProviders}/${totalProviders}" +
            " | $providerSummary"
    }

    fun completedDiagnostics(
        resultDiagnostics: String?,
        sources: List<SourceResult>,
        providerSummary: String,
        buildDiagnostics: (List<SourceResult>) -> String
    ): String {
        return (resultDiagnostics ?: buildDiagnostics(sources)) + " | $providerSummary"
    }

    fun completedStatusMessage(resultCount: Int, searchLabel: String, error: String?): String {
        return error ?: "Found $resultCount source(s) for $searchLabel."
    }
}
