package ai.shieldtv.app.update

import java.io.File
import java.net.URL

class ApkDownloadManager {
    fun download(url: String, destinationFile: File): File {
        destinationFile.parentFile?.mkdirs()
        URL(url).openStream().use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destinationFile
    }
}
