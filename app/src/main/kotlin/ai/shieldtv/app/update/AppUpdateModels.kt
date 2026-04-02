package ai.shieldtv.app.update

data class AppUpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val pageUrl: String,
    val publishedAt: String? = null,
    val notes: String? = null
)
