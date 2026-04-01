package ai.shieldtv.app.integration.scrapers.provider.template

import ai.shieldtv.app.domain.provider.RawProviderSource

interface ProviderParser {
    fun parse(rawResponse: String): List<RawProviderSource>
}
