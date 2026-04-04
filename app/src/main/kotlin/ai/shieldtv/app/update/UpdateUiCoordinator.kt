package ai.shieldtv.app.update

import ai.shieldtv.app.errors.UpdateFlowError
import java.io.File

data class UpdateCheckUiResult(
    val updateInfo: AppUpdateInfo?,
    val statusMessage: String,
    val errorMessage: String? = null,
    val errorType: String? = null
)

sealed class UpdateInstallUiResult {
    data class Ready(
        val readiness: UpdateInstallReadiness,
        val statusMessage: String
    ) : UpdateInstallUiResult()

    data class RequiresSettings(
        val readiness: UpdateInstallReadiness,
        val statusMessage: String
    ) : UpdateInstallUiResult()

    data class Unavailable(
        val readiness: UpdateInstallReadiness,
        val statusMessage: String
    ) : UpdateInstallUiResult()

    data class Failure(
        val statusMessage: String,
        val errorMessage: String,
        val errorType: String
    ) : UpdateInstallUiResult()
}

class UpdateUiCoordinator(
    private val updateCoordinator: UpdateCoordinator,
    private val cacheDirProvider: () -> File
) {
    suspend fun checkForUpdates(): UpdateCheckUiResult {
        return runCatching { updateCoordinator.checkForUpdates() }
            .fold(
                onSuccess = { result ->
                    UpdateCheckUiResult(
                        updateInfo = result.updateInfo,
                        statusMessage = result.statusMessage
                    )
                },
                onFailure = { error ->
                    val message = buildString {
                        append("Update check failed")
                        error::class.simpleName?.let {
                            append(" (")
                            append(it)
                            append(")")
                        }
                        error.message?.takeIf { it.isNotBlank() }?.let {
                            append(": ")
                            append(it.take(180))
                        }
                    }
                    UpdateCheckUiResult(
                        updateInfo = null,
                        statusMessage = message,
                        errorMessage = message,
                        errorType = UpdateFlowError.CheckFailed::class.simpleName
                    )
                }
            )
    }

    suspend fun prepareInstall(updateInfo: AppUpdateInfo): UpdateInstallUiResult {
        return runCatching {
            updateCoordinator.prepareInstall(updateInfo, cacheDirProvider())
        }.fold(
            onSuccess = { readiness ->
                when {
                    readiness.canInstall -> UpdateInstallUiResult.Ready(
                        readiness = readiness,
                        statusMessage = "Launching package installer…"
                    )
                    readiness.openSettingsIntent != null -> UpdateInstallUiResult.RequiresSettings(
                        readiness = readiness,
                        statusMessage = "Enable install unknown apps for Asahi."
                    )
                    else -> UpdateInstallUiResult.Unavailable(
                        readiness = readiness,
                        statusMessage = readiness.message ?: "Update install unavailable."
                    )
                }
            },
            onFailure = { error ->
                UpdateInstallUiResult.Failure(
                    statusMessage = "Update download/install failed: ${error.message ?: error::class.simpleName}",
                    errorMessage = error.message ?: error::class.simpleName ?: "Unknown update error.",
                    errorType = UpdateFlowError.DownloadFailed::class.simpleName ?: "DownloadFailed"
                )
            }
        )
    }
}
