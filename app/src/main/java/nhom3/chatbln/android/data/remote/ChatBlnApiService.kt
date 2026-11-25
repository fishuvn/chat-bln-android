package nhom3.chatbln.android.data.remote

import nhom3.chatbln.android.data.model.AuthResponse
import nhom3.chatbln.android.data.model.ConversationItemResponse
import nhom3.chatbln.android.data.model.ConversationResponse
import nhom3.chatbln.android.data.model.CreateGroupConversationRequest
import nhom3.chatbln.android.data.model.CreatePrivateConversationRequest
import nhom3.chatbln.android.data.model.DeleteMessagesRequest
import nhom3.chatbln.android.data.model.DeleteMessagesResponse
import nhom3.chatbln.android.data.model.FriendRequestDto
import nhom3.chatbln.android.data.model.FriendResponse
import nhom3.chatbln.android.data.model.FriendshipResponse
import nhom3.chatbln.android.data.model.GroupInvitePreviewResponse
import nhom3.chatbln.android.data.model.GroupJoinRequestDto
import nhom3.chatbln.android.data.model.GroupMemberDto
import nhom3.chatbln.android.data.model.JoinGroupResponse
import nhom3.chatbln.android.data.model.LoginRequest
import nhom3.chatbln.android.data.model.LogoutResponse
import nhom3.chatbln.android.data.model.MessageDto
import nhom3.chatbln.android.data.model.MessageHistoryResponse
import nhom3.chatbln.android.data.model.RegenerateInviteCodeResponse
import nhom3.chatbln.android.data.model.RegisterRequest
import nhom3.chatbln.android.data.model.SendMessageRequest
import nhom3.chatbln.android.data.model.TransferGroupLeaderRequest
import nhom3.chatbln.android.data.model.UpdateGroupVisibilityRequest
import nhom3.chatbln.android.data.model.UpdateMemberRoleRequest
import nhom3.chatbln.android.data.model.UpdatePresenceRequest
import nhom3.chatbln.android.data.model.UpdateProfileRequest
import nhom3.chatbln.android.data.model.UploadResponse
import nhom3.chatbln.android.data.model.UserProfileResponse
import nhom3.chatbln.android.data.model.UserSearchResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatBlnApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(): LogoutResponse

    @GET("users/me")
    suspend fun getMyProfile(): UserProfileResponse

    @PUT("users/me")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): UserProfileResponse

    @PUT("users/me/presence")
    suspend fun updateMyPresence(@Body request: UpdatePresenceRequest): UserProfileResponse

    @GET("users/search")
    suspend fun searchUsers(@Query("keyword") keyword: String): List<UserSearchResponse>

    @GET("conversations")
    suspend fun getConversations(): List<ConversationItemResponse>

    @POST("conversations/private")
    suspend fun createPrivateConversation(
        @Body request: CreatePrivateConversationRequest
    ): ConversationResponse

    @POST("conversations/group")
    suspend fun createGroupConversation(
        @Body request: CreateGroupConversationRequest
    ): ConversationResponse

    @DELETE("conversations/{id}")
    suspend fun deleteConversation(
        @Path("id") conversationId: Long
    ): Map<String, Any>

    @GET("conversations/{id}/members")
    suspend fun getConversationMembers(
        @Path("id") conversationId: Long
    ): List<GroupMemberDto>

    @PUT("conversations/{id}/visibility")
    suspend fun updateGroupVisibility(
        @Path("id") conversationId: Long,
        @Body request: UpdateGroupVisibilityRequest
    ): ConversationResponse

    @POST("conversations/{id}/invite-code/regenerate")
    suspend fun regenerateInviteCode(
        @Path("id") conversationId: Long,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): RegenerateInviteCodeResponse

    @PUT("conversations/{id}/members/{targetUserId}/role")
    suspend fun updateGroupMemberRole(
        @Path("id") conversationId: Long,
        @Path("targetUserId") targetUserId: Long,
        @Body request: UpdateMemberRoleRequest
    ): List<GroupMemberDto>

    @PUT("conversations/{id}/leader")
    suspend fun transferGroupLeadership(
        @Path("id") conversationId: Long,
        @Body request: TransferGroupLeaderRequest
    ): List<GroupMemberDto>

    @GET("conversations/invite/{inviteCode}")
    suspend fun getGroupInvitePreview(
        @Path("inviteCode") inviteCode: String
    ): GroupInvitePreviewResponse

    @POST("conversations/join/{inviteCode}")
    suspend fun joinGroupByInviteCode(
        @Path("inviteCode") inviteCode: String,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): JoinGroupResponse

    @GET("conversations/{id}/join-requests")
    suspend fun getGroupJoinRequests(
        @Path("id") conversationId: Long
    ): List<GroupJoinRequestDto>

    @PUT("conversations/{id}/join-requests/{requestId}/approve")
    suspend fun approveGroupJoinRequest(
        @Path("id") conversationId: Long,
        @Path("requestId") requestId: Long,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): GroupJoinRequestDto

    @PUT("conversations/{id}/join-requests/{requestId}/reject")
    suspend fun rejectGroupJoinRequest(
        @Path("id") conversationId: Long,
        @Path("requestId") requestId: Long,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): GroupJoinRequestDto

    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: Long,
        @Query("before") before: Long? = null,
        @Query("limit") limit: Int = 30
    ): MessageHistoryResponse

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") conversationId: Long,
        @Body request: SendMessageRequest
    ): MessageDto

    @POST("conversations/{id}/messages/delete")
    suspend fun deleteMessages(
        @Path("id") conversationId: Long,
        @Body request: DeleteMessagesRequest
    ): DeleteMessagesResponse

    @Multipart
    @POST("uploads")
    suspend fun uploadFile(@Part file: MultipartBody.Part): UploadResponse

    @GET("friends")
    suspend fun getFriends(): List<FriendResponse>

    @GET("friends/requests/received")
    suspend fun getReceivedFriendRequests(): List<FriendshipResponse>

    @GET("friends/requests/sent")
    suspend fun getSentFriendRequests(): List<FriendshipResponse>

    @POST("friends/request")
    suspend fun sendFriendRequest(@Body request: FriendRequestDto): FriendshipResponse

    @PUT("friends/{id}/accept")
    suspend fun acceptFriendRequest(
        @Path("id") friendshipId: Long,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): FriendshipResponse

    @PUT("friends/{id}/reject")
    suspend fun rejectFriendRequest(
        @Path("id") friendshipId: Long,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): FriendshipResponse

    @DELETE("friends/requests/{id}")
    suspend fun cancelSentFriendRequest(
        @Path("id") friendshipId: Long
    ): Map<String, Any>

    @DELETE("friends/{targetUserId}")
    suspend fun unfriend(
        @Path("targetUserId") targetUserId: Long
    ): Map<String, Any>
}
