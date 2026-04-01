package ai.shieldtv.app.core.network.http

import java.net.http.HttpClient
import java.time.Duration

object HttpClientFactory {
    fun createDefault(): HttpClient {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    }
}
