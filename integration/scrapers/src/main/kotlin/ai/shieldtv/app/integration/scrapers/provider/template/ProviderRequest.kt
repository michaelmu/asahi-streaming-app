package ai.shieldtv.app.integration.scrapers.provider.template

data class ProviderRequest(
    val query: String,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap()
)
