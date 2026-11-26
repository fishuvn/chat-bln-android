package nhom3.chatbln.android.data.remote

import nhom3.chatbln.android.data.local.SessionPreferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class DynamicBaseUrlInterceptor(
    private val sessionPreferences: SessionPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // Only rewrite requests targeting the configured placeholder origin (10.0.2.2:8088)
        if (originalUrl.host != "10.0.2.2" || originalUrl.port != 8088) {
            return chain.proceed(originalRequest)
        }

        val targetBase = sessionPreferences.currentBaseUrl.toHttpUrlOrNull()
            ?: return chain.proceed(originalRequest)

        val baseSegments = targetBase.encodedPathSegments.filter { it.isNotEmpty() }
        val requestSegments = originalUrl.encodedPathSegments.filter { it.isNotEmpty() }

        val rebuiltUrlBuilder = originalUrl.newBuilder()
            .scheme(targetBase.scheme)
            .host(targetBase.host)
            .port(targetBase.port)
            .encodedPath("/")

        baseSegments.forEach { segment ->
            rebuiltUrlBuilder.addEncodedPathSegment(segment)
        }
        requestSegments.forEach { segment ->
            rebuiltUrlBuilder.addEncodedPathSegment(segment)
        }

        val updatedRequest = originalRequest.newBuilder()
            .url(rebuiltUrlBuilder.build())
            .build()

        return chain.proceed(updatedRequest)
    }
}
