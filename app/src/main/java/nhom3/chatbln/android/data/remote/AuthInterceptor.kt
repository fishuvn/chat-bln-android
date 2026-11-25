package nhom3.chatbln.android.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import nhom3.chatbln.android.data.local.SessionPreferences
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionPreferences: SessionPreferences
) : Interceptor {

    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val encodedPath = originalRequest.url.encodedPath

        val isPublicEndpoint = encodedPath.endsWith("/api/auth/login") ||
            encodedPath.endsWith("/api/auth/register") ||
            encodedPath.endsWith("/api/health")

        val token = sessionPreferences.currentToken
        val requestToProceed = if (!isPublicEndpoint && !token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(requestToProceed)
        if (response.code == 401 && !isPublicEndpoint && !encodedPath.endsWith("/api/auth/logout")) {
            _unauthorizedEvents.tryEmit(Unit)
        }
        return response
    }
}
