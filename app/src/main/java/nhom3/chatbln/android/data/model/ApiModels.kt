package nhom3.chatbln.android.data.model

import com.google.gson.annotations.SerializedName

fun formatGroupRoleLabel(role: String?): String = when (role?.trim()?.uppercase()) {
    "ADMIN" -> "Trưởng nhóm"
    "DEPUTY" -> "Phó nhóm"
    else -> "Thành viên"
}

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
    @SerializedName("presenceStatus") val presenceStatus: String? = null,
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

data class UpdatePresenceRequest(
    @SerializedName("presenceStatus") val presenceStatus: String
)

data class UserSearchResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName(value = "isOnline", alternate = ["online"]) val isOnline: Boolean? = false,
    @SerializedName("presenceStatus") val presenceStatus: String? = null,
    @SerializedName(value = "isFriend", alternate = ["friend"]) val isFriend: Boolean? = null,
    @SerializedName("friendshipStatus") val friendshipStatus: String? = null
)

data class ConversationItemResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String = "PRIVATE",
    @SerializedName("groupVisibility") val groupVisibility: String? = null,
    @SerializedName("inviteCode") val inviteCode: String? = null,
    @SerializedName("pendingJoinRequestCount") val pendingJoinRequestCount: Int? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName(value = "isOnline", alternate = ["online"]) val isOnline: Boolean? = false,
    @SerializedName("presenceStatus") val presenceStatus: String? = null,
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
    @SerializedName("lastMessageTime") val lastMessageTime: String? = null,
    @SerializedName("currentUserRole") val currentUserRole: String? = null,
    @SerializedName("memberCount") val memberCount: Int? = null,
    @SerializedName("members") val members: List<GroupMemberDto>? = null,
    @SerializedName(value = "isFriend", alternate = ["friend"]) val isFriend: Boolean? = null,
    @SerializedName("friendshipStatus") val friendshipStatus: String? = null
) {
    val isGroup: Boolean
        get() = type.equals("GROUP", ignoreCase = true)

    val isCurrentUserGroupAdmin: Boolean
        get() = currentUserRole.equals("ADMIN", ignoreCase = true)

    val isCurrentUserGroupDeputy: Boolean
        get() = currentUserRole.equals("DEPUTY", ignoreCase = true)

    val canManageGroupInvitesAndRequests: Boolean
        get() = isCurrentUserGroupAdmin || isCurrentUserGroupDeputy

    val isPrivateGroup: Boolean
        get() = groupVisibility.equals("PRIVATE", ignoreCase = true)

    val groupVisibilityLabel: String
        get() = if (isPrivateGroup) "Nhóm riêng tư" else "Nhóm công khai"

    val currentUserRoleLabel: String
        get() = formatGroupRoleLabel(currentUserRole)

    val isUnfriendedPrivate: Boolean
        get() = !isGroup && isFriend == false
}

data class GroupMemberDto(
    @SerializedName("id") val id: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName(value = "isOnline", alternate = ["online"]) val isOnline: Boolean = false,
    @SerializedName("presenceStatus") val presenceStatus: String? = null,
    @SerializedName("lastSeen") val lastSeen: String? = null,
    @SerializedName("joinedAt") val joinedAt: String? = null
) {
    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() } ?: username

    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true)

    val isDeputy: Boolean
        get() = role.equals("DEPUTY", ignoreCase = true)

    val roleLabel: String
        get() = formatGroupRoleLabel(role)
}

data class FriendshipEventDto(
    @SerializedName("type") val type: String? = null,
    @SerializedName("actorUserId") val actorUserId: Long? = null,
    @SerializedName("targetUserId") val targetUserId: Long? = null
)

data class DeleteMessagesRequest(
    @SerializedName("messageIds") val messageIds: List<Long>,
    @SerializedName("mode") val mode: String
)

data class DeleteMessagesResponse(
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("deletedIds") val deletedIds: List<Long> = emptyList(),
    @SerializedName("lastMessage") val lastMessage: String? = null,
    @SerializedName("lastMessageSenderName") val lastMessageSenderName: String? = null,
    @SerializedName("lastMessageSenderUsername") val lastMessageSenderUsername: String? = null,
    @SerializedName("lastMessageTime") val lastMessageTime: String? = null
)

data class CreatePrivateConversationRequest(
    @SerializedName("targetUserId") val targetUserId: Long
)

data class CreateGroupConversationRequest(
    @SerializedName("name") val name: String,
    @SerializedName("memberIds") val memberIds: List<Long> = emptyList(),
    @SerializedName("groupVisibility") val groupVisibility: String = "PUBLIC"
)

data class ConversationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String = "PRIVATE",
    @SerializedName("groupVisibility") val groupVisibility: String? = null,
    @SerializedName("inviteCode") val inviteCode: String? = null,
    @SerializedName("createdById") val createdById: Long? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class UpdateGroupVisibilityRequest(
    @SerializedName("groupVisibility") val groupVisibility: String
)

data class RegenerateInviteCodeResponse(
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("inviteCode") val inviteCode: String,
    @SerializedName("groupVisibility") val groupVisibility: String? = null
)

data class UpdateMemberRoleRequest(
    @SerializedName("role") val role: String
)

data class TransferGroupLeaderRequest(
    @SerializedName("newLeaderUserId") val newLeaderUserId: Long
)

data class GroupInvitePreviewResponse(
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("groupVisibility") val groupVisibility: String = "PUBLIC",
    @SerializedName("inviteCode") val inviteCode: String,
    @SerializedName("memberCount") val memberCount: Int = 0,
    @SerializedName(value = "alreadyMember", alternate = ["isAlreadyMember"]) val alreadyMember: Boolean = false,
    @SerializedName("joinStatus") val joinStatus: String = "NONE"
) {
    val isPrivateGroup: Boolean
        get() = groupVisibility.equals("PRIVATE", ignoreCase = true)
}

data class JoinGroupResponse(
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("groupVisibility") val groupVisibility: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null
)

data class GroupJoinRequestDto(
    @SerializedName("id") val id: Long,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("username") val username: String,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("status") val status: String = "PENDING",
    @SerializedName("createdAt") val createdAt: String? = null
) {
    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() } ?: username
}

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
    @SerializedName("presenceStatus") val presenceStatus: String? = null,
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
    @SerializedName("presenceStatus") val presenceStatus: String? = null,
    @SerializedName("lastSeen") val lastSeen: String? = null
)

data class FriendshipResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("requesterId") val requesterId: Long,
    @SerializedName("requesterUsername") val requesterUsername: String? = null,
    @SerializedName("requesterFullName") val requesterFullName: String? = null,
    @SerializedName("requesterAvatarUrl") val requesterAvatarUrl: String? = null,
    @SerializedName("addresseeId") val addresseeId: Long,
    @SerializedName("addresseeUsername") val addresseeUsername: String? = null,
    @SerializedName("addresseeFullName") val addresseeFullName: String? = null,
    @SerializedName("addresseeAvatarUrl") val addresseeAvatarUrl: String? = null,
    @SerializedName("status") val status: String = "PENDING",
    @SerializedName("createdAt") val createdAt: String? = null
)

data class ApiErrorResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null
)
