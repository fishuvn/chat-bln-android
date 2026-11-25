package nhom3.chatbln.android.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nhom3.chatbln.android.data.local.SessionPreferences
import nhom3.chatbln.android.data.model.ApiErrorResponse
import nhom3.chatbln.android.data.model.AuthResponse
import nhom3.chatbln.android.data.model.CachedUser
import nhom3.chatbln.android.data.model.ConversationItemResponse
import nhom3.chatbln.android.data.model.ConversationResponse
import nhom3.chatbln.android.data.model.CreateGroupConversationRequest
import nhom3.chatbln.android.data.model.CreatePrivateConversationRequest
import nhom3.chatbln.android.data.model.FriendRequestDto
import nhom3.chatbln.android.data.model.FriendResponse
import nhom3.chatbln.android.data.model.FriendshipResponse
import nhom3.chatbln.android.data.model.LoginRequest
import nhom3.chatbln.android.data.model.MessageDto
import nhom3.chatbln.android.data.model.MessageHistoryResponse
import nhom3.chatbln.android.data.model.RegisterRequest
import nhom3.chatbln.android.data.model.SendMessageRequest
import nhom3.chatbln.android.data.model.UpdateProfileRequest
import nhom3.chatbln.android.data.model.UploadResponse
import nhom3.chatbln.android.data.model.UserProfileResponse
import nhom3.chatbln.android.data.model.UserSearchResponse
import nhom3.chatbln.android.data.remote.ChatBlnApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale

class ChatRepository(
    context: Context,
    private val apiService: ChatBlnApiService,
    private val sessionPreferences: SessionPreferences,
    private val gson: Gson = Gson()
) {
    private val appContext = context.applicationContext

    suspend fun login(username: String, password: String): Result<AuthResponse> = safeApiCall {
        val response = apiService.login(
            LoginRequest(username = username.trim(), password = password)
        )
        val cachedUser = CachedUser(
            id = response.id,
            username = response.username,
            fullName = response.fullName?.takeIf { it.isNotBlank() } ?: response.username
        )
        sessionPreferences.saveAuthSession(token = response.token, user = cachedUser)
        runCatching {
            val fullProfile = apiService.getMyProfile()
            sessionPreferences.updateCachedUser(fullProfile.toCachedUser())
        }
        response
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> = safeApiCall {
        val response = apiService.register(request)
        val cachedUser = CachedUser(
            id = response.id,
            username = response.username,
            fullName = response.fullName?.takeIf { it.isNotBlank() } ?: request.fullName,
            email = request.email,
            phone = request.phone,
            gender = request.gender,
            dob = request.dob,
            bio = request.bio
        )
        sessionPreferences.saveAuthSession(token = response.token, user = cachedUser)
        runCatching {
            val fullProfile = apiService.getMyProfile()
            sessionPreferences.updateCachedUser(fullProfile.toCachedUser())
        }
        response
    }

    suspend fun logout(): Result<Unit> {
        runCatching { apiService.logout() }
        sessionPreferences.clearAuthSession()
        return Result.success(Unit)
    }

    suspend fun getMyProfile(): Result<UserProfileResponse> = safeApiCall {
        val profile = apiService.getMyProfile()
        sessionPreferences.updateCachedUser(profile.toCachedUser())
        profile
    }

    suspend fun updateMyProfile(request: UpdateProfileRequest): Result<UserProfileResponse> =
        safeApiCall {
            val updated = apiService.updateMyProfile(request)
            sessionPreferences.updateCachedUser(updated.toCachedUser())
            updated
        }

    suspend fun searchUsers(keyword: String): Result<List<UserSearchResponse>> = safeApiCall {
        val normalized = keyword.trim().trimStart('@')
        if (normalized.isEmpty()) {
            emptyList()
        } else {
            apiService.searchUsers(normalized)
        }
    }

    suspend fun getConversations(): Result<List<ConversationItemResponse>> = safeApiCall {
        apiService.getConversations()
    }

    suspend fun createPrivateConversation(targetUserId: Long): Result<ConversationResponse> =
        safeApiCall {
            apiService.createPrivateConversation(
                CreatePrivateConversationRequest(targetUserId = targetUserId)
            )
        }

    suspend fun createGroupConversation(
        name: String,
        memberIds: List<Long>
    ): Result<ConversationResponse> = safeApiCall {
        apiService.createGroupConversation(
            CreateGroupConversationRequest(name = name.trim(), memberIds = memberIds)
        )
    }

    suspend fun getMessages(
        conversationId: Long,
        before: Long? = null,
        limit: Int = 30
    ): Result<MessageHistoryResponse> = safeApiCall {
        apiService.getMessages(
            conversationId = conversationId,
            before = before,
            limit = limit
        )
    }

    suspend fun sendMessage(
        conversationId: Long,
        request: SendMessageRequest
    ): Result<MessageDto> = safeApiCall {
        apiService.sendMessage(conversationId = conversationId, request = request)
    }

    suspend fun getFriends(): Result<List<FriendResponse>> = safeApiCall {
        apiService.getFriends()
    }

    suspend fun getReceivedFriendRequests(): Result<List<FriendshipResponse>> = safeApiCall {
        apiService.getReceivedFriendRequests()
    }

    suspend fun getSentFriendRequests(): Result<List<FriendshipResponse>> = safeApiCall {
        apiService.getSentFriendRequests()
    }

    suspend fun sendFriendRequest(targetUserId: Long): Result<FriendshipResponse> = safeApiCall {
        apiService.sendFriendRequest(FriendRequestDto(targetUserId = targetUserId))
    }

    suspend fun acceptFriendRequest(friendshipId: Long): Result<FriendshipResponse> = safeApiCall {
        apiService.acceptFriendRequest(friendshipId = friendshipId)
    }

    suspend fun rejectFriendRequest(friendshipId: Long): Result<FriendshipResponse> = safeApiCall {
        apiService.rejectFriendRequest(friendshipId = friendshipId)
    }

    suspend fun uploadFromUri(uri: Uri, fallbackMime: String? = null): Result<UploadResponse> =
        withContext(Dispatchers.IO) {
            safeApiCall {
                val resolver = appContext.contentResolver
                val metadata = queryUriMetadata(resolver, uri)
                if (metadata.sizeBytes != null && metadata.sizeBytes > MAX_UPLOAD_BYTES) {
                    throw IllegalArgumentException("Tệp vượt quá giới hạn dung lượng 50 MB.")
                }

                val detectedMime = resolver.getType(uri)
                    ?: inferMimeFromFileName(metadata.displayName)
                    ?: fallbackMime
                    ?: "application/octet-stream"

                val safeFileName = ensureExtensionIfNeeded(metadata.displayName, detectedMime)
                val mediaType = detectedMime.toMediaTypeOrNull()

                val requestBody = object : RequestBody() {
                    override fun contentType() = mediaType

                    override fun contentLength(): Long = metadata.sizeBytes ?: -1L

                    override fun writeTo(sink: BufferedSink) {
                        val inputStream = resolver.openInputStream(uri)
                            ?: throw IOException("Không thể đọc dữ liệu từ tệp đã chọn.")
                        inputStream.use { stream ->
                            sink.writeAll(stream.source())
                        }
                    }
                }

                val part = MultipartBody.Part.createFormData(
                    "file",
                    safeFileName,
                    requestBody
                )
                apiService.uploadFile(part)
            }
        }

    /**
     * Resolves backend relative `/uploads/...` URLs against the active Server Base URL,
     * while ignoring web-only static SVG avatar placeholders (`/avatars/...`) so the native UI
     * renders clean Material 3 monogram initials instead of broken URLs.
     */
    fun resolveAssetUrl(rawUrl: String?, baseUrlOverride: String? = null): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("/avatars/", ignoreCase = true)) {
            return null
        }
        if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            return trimmed
        }
        val origin = SessionPreferences.normalizeServerUrl(
            baseUrlOverride ?: sessionPreferences.currentBaseUrl
        )
        return if (trimmed.startsWith("/")) "$origin$trimmed" else "$origin/$trimmed"
    }

    private data class UriMetadata(
        val displayName: String,
        val sizeBytes: Long?
    )

    private fun queryUriMetadata(resolver: ContentResolver, uri: Uri): UriMetadata {
        var name: String? = null
        var size: Long? = null
        runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        name = cursor.getString(nameIdx)
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        }
        val fallbackName = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "attachment_${System.currentTimeMillis()}"

        return UriMetadata(
            displayName = name?.takeIf { it.isNotBlank() } ?: fallbackName,
            sizeBytes = size?.takeIf { it >= 0L }
        )
    }

    private fun inferMimeFromFileName(fileName: String): String? {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun ensureExtensionIfNeeded(fileName: String, mimeType: String): String {
        if (fileName.contains('.')) return fileName
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        return if (!ext.isNullOrBlank()) "$fileName.$ext" else fileName
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (httpEx: HttpException) {
            val rawBody = runCatching { httpEx.response()?.errorBody()?.string() }.getOrNull()
            val parsedMessage = rawBody
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { gson.fromJson(it, ApiErrorResponse::class.java) }.getOrNull() }
                ?.message
                ?.takeIf { it.isNotBlank() }

            val fallback = when (httpEx.code()) {
                401 -> "Thông tin đăng nhập không hợp lệ hoặc phiên đã hết hạn."
                403 -> "Bạn không có quyền thực hiện thao tác này."
                404 -> "Không tìm thấy dữ liệu yêu cầu trên máy chủ."
                413 -> "Tệp tải lên quá lớn so với giới hạn máy chủ."
                else -> "Lỗi máy chủ (HTTP ${httpEx.code()})."
            }
            Result.failure(IllegalStateException(parsedMessage ?: fallback, httpEx))
        } catch (connectEx: ConnectException) {
            Result.failure(
                IllegalStateException(
                    "Không kết nối được tới máy chủ (${sessionPreferences.currentBaseUrl}). " +
                        "Hãy kiểm tra Backend cổng 8088 hoặc đổi địa chỉ Server URL.",
                    connectEx
                )
            )
        } catch (timeoutEx: SocketTimeoutException) {
            Result.failure(
                IllegalStateException(
                    "Kết nối tới máy chủ (${sessionPreferences.currentBaseUrl}) quá thời gian chờ.",
                    timeoutEx
                )
            )
        } catch (hostEx: UnknownHostException) {
            Result.failure(
                IllegalStateException(
                    "Không phân giải được địa chỉ máy chủ (${sessionPreferences.currentBaseUrl}).",
                    hostEx
                )
            )
        } catch (ex: Exception) {
            Result.failure(
                IllegalStateException(
                    ex.message?.takeIf { it.isNotBlank() } ?: "Đã xảy ra lỗi không xác định.",
                    ex
                )
            )
        }
    }

    companion object {
        private const val MAX_UPLOAD_BYTES = 50L * 1024L * 1024L
    }
}
