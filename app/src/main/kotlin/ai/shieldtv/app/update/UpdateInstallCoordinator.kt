package ai.shieldtv.app.update

import android.content.Intent
import java.io.File

class UpdateInstallCoordinator(
    private val downloader: (String, File) -> File,
    private val installerIntentBuilder: (File) -> Intent
) {
    fun downloadAndBuildInstallIntent(url: String, destinationFile: File): Intent {
        val apkFile = downloader(url, destinationFile)
        require(apkFile.exists() && apkFile.length() > 0L) { "Downloaded APK missing or empty" }
        return installerIntentBuilder(apkFile)
    }
}
