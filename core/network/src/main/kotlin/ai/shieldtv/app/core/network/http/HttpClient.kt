package ai.shieldtv.app.core.network.http

interface HttpClient {
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): String

    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): String
}
