package ai.shieldtv.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateCheckResult(
    val updateInfo: AppUpdateInfo? = null,
    val statusMessage: String
)

class GitHubReleaseUpdateChecker(
    private val owner: String,
    private val repo: String,
    private val currentVersionName: String
) {
    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCheck()
    }

    private fun runCheck(): UpdateCheckResult {
        val releases = fetchReleases()
        if (releases.length() == 0) {
            return UpdateCheckResult(statusMessage = "No GitHub releases found.")
        }

        for (index in 0 until releases.length()) {
            val json = releases.optJSONObject(index) ?: continue
            val latestVersion = json.optString("tag_name").ifBlank {
                json.optString("name")
            }.removePrefix("v")

            if (latestVersion.isBlank()) continue

            val assets = json.optJSONArray("assets") ?: JSONArray()
            val apkAsset = findApkAsset(assets) ?: return UpdateCheckResult(
                statusMessage = "Release $latestVersion found, but no APK asset was attached."
            )
            val isDebugRollingRelease = latestVersion.equals("latest-debug", ignoreCase = true)
            if (!isDebugRollingRelease && !isNewerThanCurrent(latestVersion)) {
                continue
            }

            return UpdateCheckResult(
                updateInfo = AppUpdateInfo(
                    versionName = latestVersion,
                    versionCodeHint = extractVersionCodeHint(json.optString("body")),
                    downloadUrl = apkAsset.optString("browser_download_url"),
                    pageUrl = json.optString("html_url"),
                    publishedAt = json.optString("published_at"),
                    notes = json.optString("body")
                ),
                statusMessage = buildString {
                    append("Update available: ")
                    append(latestVersion)
                    extractVersionCodeHint(json.optString("body"))?.let {
                        append(" (versionCode ")
                        append(it)
                        append(")")
                    }
                }
            )
        }

        return UpdateCheckResult(statusMessage = "No newer release found.")
    }

    private fun fetchReleases(): JSONArray {
        val connection = URL("https://api.github.com/repos/$owner/$repo/releases").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "AsahiUpdateChecker")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000

        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.let { input ->
            BufferedReader(InputStreamReader(input)).use { reader -> reader.readText() }
        }.orEmpty()

        if (statusCode !in 200..299) {
            throw IllegalStateException("GitHub update check failed ($statusCode): ${body.take(120)}")
        }

        return JSONArray(body)
    }

    private fun findApkAsset(assets: JSONArray): JSONObject? {
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                return asset
            }
        }
        return null
    }

    private fun isNewerThanCurrent(latestVersion: String): Boolean {
        val latest = normalizeVersion(latestVersion)
        val current = normalizeVersion(currentVersionName)
        val maxSize = maxOf(latest.size, current.size)
        for (index in 0 until maxSize) {
            val latestPart = latest.getOrElse(index) { 0 }
            val currentPart = current.getOrElse(index) { 0 }
            if (latestPart != currentPart) {
                return latestPart > currentPart
            }
        }
        return false
    }

    private fun extractVersionCodeHint(notes: String?): String? {
        if (notes.isNullOrBlank()) return null
        val regex = Regex("Version code:\\s*`?(\\d+)`?", RegexOption.IGNORE_CASE)
        return regex.find(notes)?.groupValues?.getOrNull(1)
    }

    private fun normalizeVersion(version: String): List<Int> {
        return version
            .substringBefore('-')
            .split('.')
            .mapNotNull { it.toIntOrNull() }
            .ifEmpty { listOf(0) }
    }
}
