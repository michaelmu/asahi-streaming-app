package ai.shieldtv.app.update

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GitHubReleaseUpdateChecker(
    private val owner: String,
    private val repo: String,
    private val currentVersionName: String
) {
    fun check(): AppUpdateInfo? {
        val connection = URL("https://api.github.com/repos/$owner/$repo/releases/latest").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "AsahiUpdateChecker")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000

        return connection.inputStream.bufferedReader().use { reader ->
            val json = JSONObject(reader.readText())
            val latestVersion = json.optString("tag_name").ifBlank {
                json.optString("name")
            }.removePrefix("v")

            if (latestVersion.isBlank() || !isNewerThanCurrent(latestVersion)) {
                return@use null
            }

            val assets = json.optJSONArray("assets") ?: JSONArray()
            val apkAsset = findApkAsset(assets) ?: return@use null
            AppUpdateInfo(
                versionName = latestVersion,
                downloadUrl = apkAsset.optString("browser_download_url"),
                pageUrl = json.optString("html_url"),
                publishedAt = json.optString("published_at"),
                notes = json.optString("body")
            )
        }
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

    private fun normalizeVersion(version: String): List<Int> {
        return version
            .substringBefore('-')
            .split('.')
            .mapNotNull { it.toIntOrNull() }
            .ifEmpty { listOf(0) }
    }
}
