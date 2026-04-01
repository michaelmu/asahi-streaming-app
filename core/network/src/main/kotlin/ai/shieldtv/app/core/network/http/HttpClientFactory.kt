package ai.shieldtv.app.core.network.http

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

object HttpClientFactory {
    fun createDefault(): HttpClient {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        return OkHttpClientAdapter(okHttpClient)
    }
}
