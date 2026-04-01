package ai.shieldtv.app.integration.scrapers.provider.template

interface ProviderTransport {
    suspend fun fetch(request: ProviderRequest): String
}
