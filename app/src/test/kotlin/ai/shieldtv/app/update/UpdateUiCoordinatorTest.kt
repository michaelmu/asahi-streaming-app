package ai.shieldtv.app.update

import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
class UpdateUiCoordinatorTest {
    @Test
    fun checkForUpdates_returns_summary_from_update_coordinator() = runBlocking {
        val coordinator = UpdateUiCoordinator(
            updateCoordinator = FakeUpdateCoordinator(
                checkResult = UpdateCheckResult(
                    updateInfo = AppUpdateInfo(
                        versionName = "1.2.3",
                        downloadUrl = "https://example.com/app.apk",
                        pageUrl = "https://example.com/release"
                    ),
                    statusMessage = "Update available"
                )
            ),
            cacheDirProvider = { createTempDirectory("update-ui").toFile() }
        )

        val result = coordinator.checkForUpdates()
        assertEquals("Update available", result.statusMessage)
        assertEquals("1.2.3", result.updateInfo?.versionName)
    }

    @Test
    fun prepareInstall_maps_ready_result() = runBlocking {
        val coordinator = UpdateUiCoordinator(
            updateCoordinator = FakeUpdateCoordinator(
                readiness = UpdateInstallReadiness(
                    canInstall = true,
                    installIntent = Intent("test.INSTALL")
                )
            ),
            cacheDirProvider = { createTempDirectory("update-ui-ready").toFile() }
        )

        val result = coordinator.prepareInstall(
            AppUpdateInfo(
                versionName = "1.2.3",
                downloadUrl = "https://example.com/app.apk",
                pageUrl = "https://example.com/release"
            )
        )

        assertTrue(result is UpdateInstallUiResult.Ready)
    }
}

private class StubApkDownloadManager : ApkDownloadManager() {
    override fun download(url: String, destinationFile: File): File {
        destinationFile.parentFile?.mkdirs()
        destinationFile.writeText("apk")
        return destinationFile
    }
}

private class StubApkInstaller : ApkInstaller(androidx.test.core.app.ApplicationProvider.getApplicationContext()) {
    override fun buildInstallIntent(apkFile: File): Intent = Intent("test.INSTALL")
    override fun canRequestPackageInstalls(): Boolean = true
    override fun canResolveInstallIntent(intent: Intent): Boolean = true
}

private class FakeUpdateCoordinator(
    private val checkResult: UpdateCheckResult = UpdateCheckResult(updateInfo = null, statusMessage = "Up to date"),
    private val readiness: UpdateInstallReadiness = UpdateInstallReadiness(canInstall = false, message = "Unavailable")
) : UpdateCoordinator(
    updateCheckerFactory = { throw UnsupportedOperationException() },
    apkDownloadManager = StubApkDownloadManager(),
    apkInstaller = StubApkInstaller()
) {
    override suspend fun checkForUpdates(): UpdateCheckResult = checkResult

    override suspend fun prepareInstall(updateInfo: AppUpdateInfo, cacheDir: File): UpdateInstallReadiness = readiness
}
