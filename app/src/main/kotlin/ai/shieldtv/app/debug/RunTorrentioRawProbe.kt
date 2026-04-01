package ai.shieldtv.app.debug

import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenProvider
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.scrapers.provider.template.ProviderRequest
import ai.shieldtv.app.integration.scrapers.provider.torrentio.TorrentioTransport
import kotlinx.coroutines.runBlocking

object RunTorrentioRawProbe {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val tokenStore = RealDebridTokenStore()
        val tokenProvider = RealDebridTokenProvider { tokenStore.get()?.accessToken }
        val transport = TorrentioTransport(tokenProvider)
        val response = transport.fetch(
            ProviderRequest(
                query = "Dune",
                params = mapOf("path" to "/stream/movie/tt1160419.json")
            )
        )
        println(response)
    }
}
