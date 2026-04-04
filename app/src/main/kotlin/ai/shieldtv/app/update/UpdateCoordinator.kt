package ai.shieldtv.app.update

import android.content.Intent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class UpdateInstallReadiness(
    val canInstall: Boolean,
    val installIntent: Intent? = null,
    val message: String? = null,
    val openSettingsIntent: Intent? = null
)

class UpdateCoordinator(
    private val updateCheckerFactory: () -> GitHubReleaseUpdateChecker,
    private val apkDownloadManager: ApkDownloadManager,
    private val apkInstaller: ApkInstaller,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun checkForUpdates(): UpdateCheckResult {
        return updateCheckerFactory().check()
    }

    suspend fun prepareInstall(
        updateInfo: AppUpdateInfo,
        cacheDir: File
    ): UpdateInstallReadiness = withContext(ioDispatcher) {
        val url = updateInfo.downloadUrl.ifBlank { updateInfo.pageUrl }
        if (url.isBlank()) {
            return@withContext UpdateInstallReadiness(
                canInstall = false,
                message = "This release does not include a downloadable APK asset."
            )
        }

        val destination = File(cacheDir, "updates/asahi-update.apk")
        val apkFile = apkDownloadManager.download(url, destination)
        val installIntent = apkInstaller.buildInstallIntent(apkFile)

        if (!apkInstaller.canRequestPackageInstalls()) {
            return@withContext UpdateInstallReadiness(
                canInstall = false,
                message = "Android is blocking installs from Asahi right now. Allow installs from this app, then try again.",
                openSettingsIntent = apkInstaller.buildManageUnknownAppsIntent()
            )
        }

        if (!apkInstaller.canResolveInstallIntent(installIntent)) {
            return@withContext UpdateInstallReadiness(
                canInstall = false,
                message = "No app on this device can handle APK installation intents right now."
            )
        }

        UpdateInstallReadiness(
            canInstall = true,
            installIntent = installIntent
        )
    }
}
