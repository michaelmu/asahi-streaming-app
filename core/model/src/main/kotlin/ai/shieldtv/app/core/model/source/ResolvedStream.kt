package ai.shieldtv.app.core.model.source

data class ResolvedStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null,
    val source: SourceResult
)
