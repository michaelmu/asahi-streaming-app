package ai.shieldtv.app.update

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateInstallCoordinatorTest {
    @Test
    fun download_and_install_intent_flow_returns_intent_for_downloaded_apk() {
        val tempDir = createTempDir(prefix = "update-install-test")
        val destination = File(tempDir, "update.apk")

        val coordinator = UpdateInstallCoordinator(
            downloader = { _, destinationFile ->
                destinationFile.parentFile?.mkdirs()
                destinationFile.writeText("fake apk bytes")
                destinationFile
            },
            installerIntentBuilder = { apkFile ->
                Intent("test.INSTALL").apply {
                    putExtra("apk_path", apkFile.absolutePath)
                }
            }
        )

        val intent = coordinator.downloadAndBuildInstallIntent("https://example.com/app.apk", destination)

        assertEquals("test.INSTALL", intent.action)
        assertTrue(File(intent.getStringExtra("apk_path")!!).exists())
    }
}
