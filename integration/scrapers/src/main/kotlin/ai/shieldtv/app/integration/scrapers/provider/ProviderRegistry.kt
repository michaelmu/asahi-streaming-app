package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.domain.provider.SourceProvider

class ProviderRegistry(
    private val providers: List<SourceProvider>
) {
    fun activeProviders(productionOnly: Boolean = true): List<SourceProvider> {
        return if (productionOnly) {
            providers.filter { it.capabilities.productionReady }
        } else {
            providers
        }
    }
}
