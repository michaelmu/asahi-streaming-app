package ai.shieldtv.app.core.network.http

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OkHttpClientAdapter(
    private val okHttpClient: OkHttpClient
) : HttpClient {
    override suspend fun get(
        url: String,
        headers: Map<String, String>
    ): String = execute(
        Request.Builder()
            .url(url)
            .applyHeaders(headers)
            .get()
            .build()
    )

    override suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String>
    ): String = execute(
        Request.Builder()
            .url(url)
            .applyHeaders(headers)
            .post(body.toRequestBody(FORM_URLENCODED_MEDIA_TYPE))
            .build()
    )

    private suspend fun execute(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = okHttpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(
            object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }

                override fun onResponse(
                    call: okhttp3.Call,
                    response: okhttp3.Response
                ) {
                    response.use {
                        val body = it.body?.string().orEmpty()
                        if (!it.isSuccessful) {
                            continuation.resumeWithException(
                                IllegalStateException("HTTP ${it.code}: $body")
                            )
                            return
                        }
                        continuation.resume(body)
                    }
                }
            }
        )
    }

    private fun Request.Builder.applyHeaders(headers: Map<String, String>): Request.Builder = apply {
        headers.forEach { (name, value) ->
            header(name, value)
        }
    }

    private companion object {
        val FORM_URLENCODED_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()
    }
}
