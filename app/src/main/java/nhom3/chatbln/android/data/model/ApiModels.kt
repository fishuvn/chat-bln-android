package nhom3.chatbln.android.data.model

import com.google.gson.annotations.SerializedName

data class CachedUser(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("bio") val bio: String? = null
)

data class AuthResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("token") val token: String,
    @SerializedName("tokenType") val tokenType: String? = "Bearer"
)

data class LogoutResponse(
    @SerializedName("status") val status: String? = null
)

data class UserProfileResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName(value = "isOnline", alternate = ["online"]) val isOnline: Boolean? = false,
    @SerializedName("lastSeen") val lastSeen: String? = null
) {
    fun toCachedUser(): CachedUser = CachedUser(
        id = id,
        username = username,
        fullName = fullName?.takeIf { it.isNotBlank() } ?: username,
        email = email,
        phone = phone,
        gender = gender,
        dob = dob,
        bio = bio,
        avatarUrl = avatarUrl
    )
}

data class UpdateProfileRequest(
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("gender") val gender: String?,
    @SerializedName("dob") val dob: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?
)

data class UserSearchResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName(value = "isOnline", alternate = ["online"]) val isOnline: Boolean? = false,
    @SerializedName(value = "isFriend", alternate = ["friend"]) val isFriend: Boolean? = null,
    @SerializedName("friendshipStatus") val friendshipStatus: String? = null
)

data class ConversationItemResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String = "PRIVATE",
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName(value = "isOnline", alternate = ["online"]) val isOnline: Boolean? = false,
    @SerializedName("targetUserId") val targetUserId: Long? = null,
    @SerializedName("targetUsername") val targetUsername: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("lastSeen") val lastSeen: String? = null,
    @SerializedName("lastMessage") val lastMessage: String? = null,
    @SerializedName("lastMessageSenderName") val lastMessageSenderName: String? = null,
    @SerializedName("lastMessageSenderUsername") val lastMessageSenderUsername: String? = null,
    @SerializedName("lastMessageTime") val lastMessageTime: String? = null
) {
    val isGroup: Boolean
        get() = type.equals("GROUP", ignoreCase = true)
}

data class CreatePrivateConversationRequest(
    @SerializedName("targetUserId") val targetUserId: Long
)

data class CreateGroupConversationRequest(
    @SerializedName("name") val name: String,
    @SerializedName("memberIds") val memberIds: List<Long>
)

data class ConversationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String = "PRIVATE",
    @SerializedName("createdById") val createdById: Long? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class SenderDto(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class MessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("sender") val sender: SenderDto,
    @SerializedName("content") val content: String,
    @SerializedName("attachmentUrl") val attachmentUrl: String? = null,
    @SerializedName("attachmentType") val attachmentType: String? = null,
    @SerializedName("attachmentName") val attachmentName: String? = null,
    @SerializedName("attachmentSize") val attachmentSize: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class MessageHistoryResponse(
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("conversationType") val conversationType: String = "PRIVATE",
    @SerializedName("messages") val messages: List<MessageDto> = emptyList(),
    @SerializedName("size") val size: Int = 0,
    @SerializedName("hasMore") val hasMore: Boolean = false,
    @SerializedName("oldestMessageId") val oldestMessageId: Long? = null
)

data class SendMessageRequest(
    @SerializedName("content") val content: String,
    @SerializedName("attachmentUrl") val attachmentUrl: String? = null,
    @SerializedName("attachmentType") val attachmentType: String? = null,
    @SerializedName("attachmentName") val attachmentName: String? = null,
    @SerializedName("attachmentSize") val attachmentSize: String? = null
)

data class UploadResponse(
    @SerializedName("url") val url: String,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("fileSize") val fileSize: String,
    @SerializedName("contentType") val contentType: String,
    @SerializedName("kind") val kind: String
)

data class PresenceEvent(
    @SerializedName("userId") val userId: Long,
    @SerializedName("username") val username: String,
    @SerializedName(value = "isOnline", alternate = ["online"]) val isOnline: Boolean = false,
    @SerializedName("lastSeen") val lastSeen: String? = null
)

data class FriendRequestDto(
    @SerializedName("targetUserId") val targetUserId: Long
)

data class FriendResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName(value = "isOnline", alternate = ["online"]) val isOnline: Boolean = false,
    @SerializedName("lastSeen") val lastSeen: String? = null
)

data class FriendshipResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("requesterId") val requesterId: Long,
    @SerializedName("requesterUsername") val requesterUsername: String? = null,
    @SerializedName("requesterFullName") val requesterFullName: String? = null,
    @SerializedName("requesterAvatarUrl") val requesterAvatarUrl: String? = null,
    @SerializedName("addresseeId") val addresseeId: Long,
    @SerializedName("status") val status: String = "PENDING",
    @SerializedName("createdAt") val createdAt: String? = null
)

data class ApiErrorResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null
)
