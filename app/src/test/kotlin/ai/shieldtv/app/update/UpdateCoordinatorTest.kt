package ai.shieldtv.app.update

import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class UpdateCoordinatorTest {
    @Test
    fun prepareInstall_returns_install_intent_when_ready() {
        val coordinator = UpdateCoordinator(
            updateCheckerFactory = { throw UnsupportedOperationException() },
            apkDownloadManager = FakeApkDownloadManager(),
            apkInstaller = FakeApkInstaller(
                canInstall = true,
                canResolve = true
            )
        )

        val readiness = runBlocking {
            coordinator.prepareInstall(updateInfo(), createTempDir(prefix = "update-ready"))
        }

        assertTrue(readiness.canInstall)
        assertNotNull(readiness.installIntent)
    }

    @Test
    fun prepareInstall_returns_settings_guidance_when_unknown_apps_blocked() {
        val coordinator = UpdateCoordinator(
            updateCheckerFactory = { throw UnsupportedOperationException() },
            apkDownloadManager = FakeApkDownloadManager(),
            apkInstaller = FakeApkInstaller(
                canInstall = false,
                canResolve = true
            )
        )

        val readiness = runBlocking {
            coordinator.prepareInstall(updateInfo(), createTempDir(prefix = "update-blocked"))
        }

        assertFalse(readiness.canInstall)
        assertNotNull(readiness.openSettingsIntent)
    }

    @Test
    fun prepareInstall_returns_message_when_no_installer_available() {
        val coordinator = UpdateCoordinator(
            updateCheckerFactory = { throw UnsupportedOperationException() },
            apkDownloadManager = FakeApkDownloadManager(),
            apkInstaller = FakeApkInstaller(
                canInstall = true,
                canResolve = false
            )
        )

        val readiness = runBlocking {
            coordinator.prepareInstall(updateInfo(), createTempDir(prefix = "update-no-installer"))
        }

        assertFalse(readiness.canInstall)
        assertEquals("No app on this device can handle APK installation intents right now.", readiness.message)
    }

    private fun updateInfo() = AppUpdateInfo(
        versionName = "1.2.3",
        downloadUrl = "https://example.com/app.apk",
        pageUrl = "https://example.com/release"
    )
}

private class FakeApkDownloadManager : ApkDownloadManager() {
    override fun download(url: String, destinationFile: File): File {
        destinationFile.parentFile?.mkdirs()
        destinationFile.writeText("apk")
        return destinationFile
    }
}

private class FakeApkInstaller(
    private val canInstall: Boolean,
    private val canResolve: Boolean
) : ApkInstaller(androidx.test.core.app.ApplicationProvider.getApplicationContext()) {
    override fun buildInstallIntent(apkFile: File): Intent = Intent("test.INSTALL")
    override fun canRequestPackageInstalls(): Boolean = canInstall
    override fun canResolveInstallIntent(intent: Intent): Boolean = canResolve
    override fun buildManageUnknownAppsIntent(): Intent = Intent("test.SETTINGS")
}
