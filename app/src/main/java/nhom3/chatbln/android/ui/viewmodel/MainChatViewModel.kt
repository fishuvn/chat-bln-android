package nhom3.chatbln.android.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nhom3.chatbln.android.AppContainer
import nhom3.chatbln.android.data.local.SessionPreferences
import nhom3.chatbln.android.data.local.UserPresenceMode
import nhom3.chatbln.android.data.model.CachedUser
import nhom3.chatbln.android.data.model.ConversationItemResponse
import nhom3.chatbln.android.data.model.DeleteMessagesResponse
import nhom3.chatbln.android.data.model.FriendResponse
import nhom3.chatbln.android.data.model.FriendshipEventDto
import nhom3.chatbln.android.data.model.FriendshipResponse
import nhom3.chatbln.android.data.model.GroupInvitePreviewResponse
import nhom3.chatbln.android.data.model.GroupJoinRequestDto
import nhom3.chatbln.android.data.model.GroupMemberDto
import nhom3.chatbln.android.data.model.MessageDto
import nhom3.chatbln.android.data.model.PresenceEvent
import nhom3.chatbln.android.data.model.SendMessageRequest
import nhom3.chatbln.android.data.model.UpdateProfileRequest
import nhom3.chatbln.android.data.model.UserProfileResponse
import nhom3.chatbln.android.data.model.UserSearchResponse
import nhom3.chatbln.android.ui.util.extractFirstLink
import java.time.LocalDateTime

enum class UserRelationshipState {
    SELF,
    FRIEND,
    SENT_PENDING,
    RECEIVED_PENDING,
    NONE
}

data class MainChatUiState(
    val currentUser: CachedUser? = null,
    val profile: UserProfileResponse? = null,
    val serverBaseUrl: String = SessionPreferences.DEFAULT_PRODUCTION_BASE_URL,
    val darkModePreference: Boolean? = null,
    val dynamicColorEnabled: Boolean = false,
    val presenceMode: UserPresenceMode = UserPresenceMode.ONLINE,
    val isWsConnected: Boolean = false,
    val conversations: List<ConversationItemResponse> = emptyList(),
    val unreadCounts: Map<Long, Int> = emptyMap(),
    val isConversationsLoading: Boolean = false,
    val conversationsError: String? = null,
    val conversationSearchQuery: String = "",
    val activeConversationId: Long? = null,
    val messagesByConversation: Map<Long, List<MessageDto>> = emptyMap(),
    val groupMembersByConversation: Map<Long, List<GroupMemberDto>> = emptyMap(),
    val groupJoinRequestsByConversation: Map<Long, List<GroupJoinRequestDto>> = emptyMap(),
    val isUpdatingGroupSettings: Boolean = false,
    val processingGroupMemberIds: Set<Long> = emptySet(),
    val processingJoinRequestIds: Set<Long> = emptySet(),
    val invitePreview: GroupInvitePreviewResponse? = null,
    val isLoadingInvitePreview: Boolean = false,
    val invitePreviewError: String? = null,
    val isJoiningGroupByInvite: Boolean = false,
    val joinGroupFeedback: String? = null,
    val joinGroupFeedbackIsError: Boolean = false,
    val hasMoreByConversation: Map<Long, Boolean> = emptyMap(),
    val oldestMessageIdByConversation: Map<Long, Long?> = emptyMap(),
    val isMessagesLoading: Boolean = false,
    val isLoadingOlderMessages: Boolean = false,
    val isSendingMessage: Boolean = false,
    val isDeletingMessages: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val inChatSearchQuery: String = "",
    val friends: List<FriendResponse> = emptyList(),
    val isFriendsLoading: Boolean = false,
    val friendsError: String? = null,
    val contactsSearchQuery: String = "",
    val receivedRequests: List<FriendshipResponse> = emptyList(),
    val sentRequests: List<FriendshipResponse> = emptyList(),
    val knownUsersById: Map<Long, UserSearchResponse> = emptyMap(),
    val isRequestsLoading: Boolean = false,
    val processingRequestIds: Set<Long> = emptySet(),
    val userSearchKeyword: String = "",
    val hasSearchedUsers: Boolean = false,
    val isSearchingUsers: Boolean = false,
    val userSearchResults: List<UserSearchResponse> = emptyList(),
    val sendingFriendRequestUserIds: Set<Long> = emptySet(),
    val addFriendFeedback: String? = null,
    val addFriendFeedbackIsError: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isCreatingConversation: Boolean = false,
    val snackbarMessage: String? = null
) {
    val totalUnreadCount: Int
        get() = unreadCounts.values.sum()

    val receivedRequestsBadgeCount: Int
        get() = receivedRequests.size
}

class MainChatViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val sessionPreferences = container.sessionPreferences
    private val repository = container.chatRepository
    private val stompClient = container.stompRealtimeClient

    private val _uiState = MutableStateFlow(
        MainChatUiState(serverBaseUrl = sessionPreferences.currentBaseUrl)
    )
    val uiState: StateFlow<MainChatUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null
    private var initializedForUsername: String? = null

    init {
        observePreferencesAndRealtime()
    }

    private fun observePreferencesAndRealtime() {
        viewModelScope.launch {
            sessionPreferences.cachedUserFlow.collectLatest { cachedUser ->
                _uiState.update { it.copy(currentUser = cachedUser) }
                val username = cachedUser?.username
                if (!username.isNullOrBlank() && initializedForUsername != username) {
                    initializedForUsername = username
                    stompClient.connect(username)
                    refreshAll()
                } else if (username.isNullOrBlank()) {
                    initializedForUsername = null
                    stompClient.disconnect()
                }
            }
        }

        viewModelScope.launch {
            sessionPreferences.presenceModeFlow.collectLatest { mode ->
                _uiState.update { it.copy(presenceMode = mode) }
            }
        }

        viewModelScope.launch {
            sessionPreferences.serverBaseUrlFlow.collectLatest { url ->
                _uiState.update { it.copy(serverBaseUrl = url) }
            }
        }

        viewModelScope.launch {
            sessionPreferences.darkModeFlow.collectLatest { darkPref ->
                _uiState.update { it.copy(darkModePreference = darkPref) }
            }
        }

        viewModelScope.launch {
            sessionPreferences.dynamicColorFlow.collectLatest { dynamicEnabled ->
                _uiState.update { it.copy(dynamicColorEnabled = dynamicEnabled) }
            }
        }

        viewModelScope.launch {
            stompClient.isConnected.collectLatest { connected ->
                _uiState.update { it.copy(isWsConnected = connected) }
            }
        }

        viewModelScope.launch {
            stompClient.incomingMessages.collectLatest { message ->
                handleIncomingRealtimeMessage(message)
            }
        }

        viewModelScope.launch {
            stompClient.presenceEvents.collectLatest { event ->
                handlePresenceEvent(event)
            }
        }

        viewModelScope.launch {
            stompClient.deletionEvents.collectLatest { event ->
                applyMessageDeletion(event)
            }
        }

        viewModelScope.launch {
            stompClient.friendshipEvents.collectLatest { event ->
                handleFriendshipOrGroupEvent(event)
            }
        }
    }

    private fun handleFriendshipOrGroupEvent(event: FriendshipEventDto) {
        val type = event.type?.uppercase().orEmpty()
        val myId = _uiState.value.currentUser?.id

        if (type == "CONVERSATION_DELETED" && event.targetUserId != null) {
            val deletedConvId = event.targetUserId
            val wasActive = _uiState.value.activeConversationId == deletedConvId
            val remaining = _uiState.value.conversations.filterNot { it.id == deletedConvId }
            stompClient.syncConversationSubscriptions(remaining.map { it.id })
            _uiState.update { state ->
                state.copy(
                    conversations = remaining,
                    messagesByConversation = state.messagesByConversation - deletedConvId,
                    groupMembersByConversation = state.groupMembersByConversation - deletedConvId,
                    groupJoinRequestsByConversation = state.groupJoinRequestsByConversation - deletedConvId,
                    hasMoreByConversation = state.hasMoreByConversation - deletedConvId,
                    oldestMessageIdByConversation = state.oldestMessageIdByConversation - deletedConvId,
                    unreadCounts = state.unreadCounts - deletedConvId,
                    snackbarMessage = if (wasActive && event.actorUserId != myId) {
                        "Nhóm hoặc cuộc trò chuyện đã được giải tán."
                    } else {
                        state.snackbarMessage
                    }
                )
            }
            refreshConversations()
            return
        }

        val isGroupEvent = type.startsWith("GROUP_") || type.startsWith("CONVERSATION_")
        if (!isGroupEvent) {
            refreshFriendsAndRequests()
            if (_uiState.value.hasSearchedUsers && _uiState.value.userSearchKeyword.isNotBlank()) {
                executeUserSearch()
            }
        }

        if (event.actorUserId != null && event.actorUserId != myId) {
            when (type) {
                "GROUP_JOIN_REQUESTED" -> {
                    _uiState.update {
                        it.copy(snackbarMessage = "Có yêu cầu tham gia nhóm mới đang chờ phê duyệt.")
                    }
                }
                "GROUP_JOIN_APPROVED" -> {
                    val targetConvId = event.targetUserId
                    val alreadyHadConv = targetConvId != null &&
                        _uiState.value.conversations.any { it.id == targetConvId }
                    if (!alreadyHadConv) {
                        _uiState.update {
                            it.copy(snackbarMessage = "Yêu cầu tham gia nhóm của bạn đã được phê duyệt.")
                        }
                    }
                }
                "GROUP_JOIN_REJECTED" -> {
                    val targetConvId = event.targetUserId
                    val alreadyHadConv = targetConvId != null &&
                        _uiState.value.conversations.any { it.id == targetConvId }
                    if (!alreadyHadConv) {
                        _uiState.update {
                            it.copy(snackbarMessage = "Yêu cầu tham gia nhóm của bạn đã bị từ chối.")
                        }
                    }
                }
            }
        }

        refreshConversations()

        val activeId = _uiState.value.activeConversationId
        val affectedConvId = event.targetUserId
        if (activeId != null && (affectedConvId == null || affectedConvId == activeId)) {
            val activeConv = _uiState.value.conversations.firstOrNull { it.id == activeId }
            if (activeConv?.isGroup == true || isGroupEvent) {
                loadConversationMembers(activeId)
            }
        }
    }

    fun refreshAll() {
        refreshProfile()
        refreshConversations()
        refreshFriendsAndRequests()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            repository.getMyProfile().onSuccess { profile ->
                val serverMode = profile.presenceStatus
                    ?.takeIf { it.isNotBlank() }
                    ?.let { UserPresenceMode.fromRaw(it) }
                if (serverMode != null) {
                    sessionPreferences.setPresenceMode(serverMode)
                }
                _uiState.update {
                    it.copy(
                        profile = profile,
                        presenceMode = serverMode ?: it.presenceMode
                    )
                }
            }
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConversationsLoading = it.conversations.isEmpty(),
                    conversationsError = null
                )
            }
            repository.getConversations().fold(
                onSuccess = { list ->
                    stompClient.syncConversationSubscriptions(list.map { c -> c.id })
                    var activeGroupToSyncRequests: ConversationItemResponse? = null
                    _uiState.update { state ->
                        val updatedMembers = state.groupMembersByConversation.toMutableMap()
                        val updatedRequests = state.groupJoinRequestsByConversation.toMutableMap()
                        list.forEach { item ->
                            if (item.isGroup && !item.members.isNullOrEmpty()) {
                                updatedMembers[item.id] = item.members
                            }
                            if (item.isGroup && !item.canManageGroupInvitesAndRequests) {
                                updatedRequests.remove(item.id)
                            }
                            if (item.id == state.activeConversationId && item.isGroup) {
                                activeGroupToSyncRequests = item
                            }
                        }
                        state.copy(
                            conversations = list,
                            groupMembersByConversation = updatedMembers,
                            groupJoinRequestsByConversation = updatedRequests,
                            isConversationsLoading = false,
                            conversationsError = null
                        )
                    }
                    activeGroupToSyncRequests?.let { activeGroup ->
                        loadConversationMembers(activeGroup.id)
                        if (activeGroup.canManageGroupInvitesAndRequests) {
                            loadGroupJoinRequests(activeGroup.id)
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isConversationsLoading = false,
                            conversationsError = err.message ?: "Không thể tải danh sách hội thoại."
                        )
                    }
                }
            )
        }
    }

    fun refreshFriendsAndRequests() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFriendsLoading = it.friends.isEmpty(),
                    isRequestsLoading = it.receivedRequests.isEmpty() && it.sentRequests.isEmpty(),
                    friendsError = null
                )
            }
            coroutineScope {
                val friendsDeferred = async { repository.getFriends() }
                val receivedDeferred = async { repository.getReceivedFriendRequests() }
                val sentDeferred = async { repository.getSentFriendRequests() }

                val friendsRes = friendsDeferred.await()
                val receivedRes = receivedDeferred.await()
                val sentRes = sentDeferred.await()

                _uiState.update { state ->
                    val updatedKnown = state.knownUsersById.toMutableMap()
                    friendsRes.getOrNull()?.forEach { friend ->
                        updatedKnown[friend.id] = UserSearchResponse(
                            id = friend.id,
                            username = friend.username,
                            fullName = friend.fullName,
                            avatarUrl = friend.avatarUrl,
                            isOnline = friend.isOnline,
                            isFriend = true,
                            friendshipStatus = "ACCEPTED"
                        )
                    }
                    state.copy(
                        friends = friendsRes.getOrElse { state.friends },
                        friendsError = friendsRes.exceptionOrNull()?.message,
                        isFriendsLoading = false,
                        receivedRequests = receivedRes.getOrElse { state.receivedRequests },
                        sentRequests = sentRes.getOrElse { state.sentRequests },
                        knownUsersById = updatedKnown,
                        isRequestsLoading = false
                    )
                }
            }
        }
    }

    fun setConversationSearchQuery(query: String) {
        _uiState.update { it.copy(conversationSearchQuery = query) }
    }

    fun setContactsSearchQuery(query: String) {
        _uiState.update { it.copy(contactsSearchQuery = query) }
    }

    fun openConversation(conversationId: Long) {
        stompClient.subscribeConversation(conversationId)
        _uiState.update { state ->
            val updatedUnread = state.unreadCounts.toMutableMap().apply { remove(conversationId) }
            state.copy(
                activeConversationId = conversationId,
                unreadCounts = updatedUnread,
                inChatSearchQuery = ""
            )
        }

        val targetConv = _uiState.value.conversations.firstOrNull { it.id == conversationId }
        if (targetConv?.isGroup == true) {
            loadConversationMembers(conversationId)
            if (targetConv.canManageGroupInvitesAndRequests) {
                loadGroupJoinRequests(conversationId)
            }
        }

        viewModelScope.launch {
            val hasLoadedBefore = _uiState.value.messagesByConversation.containsKey(conversationId)
            _uiState.update { it.copy(isMessagesLoading = !hasLoadedBefore) }
            repository.getMessages(conversationId = conversationId, before = null, limit = 30).fold(
                onSuccess = { history ->
                    val isGroupHistory = history.conversationType.equals("GROUP", ignoreCase = true)
                    _uiState.update { state ->
                        val updatedMap = state.messagesByConversation.toMutableMap()
                        updatedMap[conversationId] = history.messages

                        val updatedHasMore = state.hasMoreByConversation.toMutableMap()
                        updatedHasMore[conversationId] = history.hasMore

                        val updatedOldest = state.oldestMessageIdByConversation.toMutableMap()
                        updatedOldest[conversationId] = history.oldestMessageId

                        state.copy(
                            messagesByConversation = updatedMap,
                            hasMoreByConversation = updatedHasMore,
                            oldestMessageIdByConversation = updatedOldest,
                            isMessagesLoading = false
                        )
                    }
                    if (isGroupHistory && targetConv == null) {
                        loadConversationMembers(conversationId)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isMessagesLoading = false,
                            snackbarMessage = err.message ?: "Không thể tải lịch sử tin nhắn."
                        )
                    }
                }
            )
        }
    }

    fun closeActiveConversation() {
        _uiState.update {
            it.copy(
                activeConversationId = null,
                inChatSearchQuery = ""
            )
        }
    }

    fun setInChatSearchQuery(query: String) {
        _uiState.update { it.copy(inChatSearchQuery = query) }
    }

    fun loadOlderMessages(conversationId: Long) {
        val state = _uiState.value
        if (state.isLoadingOlderMessages) return
        if (state.hasMoreByConversation[conversationId] != true) return
        val beforeId = state.oldestMessageIdByConversation[conversationId] ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOlderMessages = true) }
            repository.getMessages(conversationId = conversationId, before = beforeId, limit = 30)
                .fold(
                    onSuccess = { history ->
                        _uiState.update { current ->
                            val existing = current.messagesByConversation[conversationId].orEmpty()
                            val existingIds = existing.map { it.id }.toHashSet()
                            val olderUnique = history.messages.filter { it.id !in existingIds }
                            val combined = olderUnique + existing

                            val updatedMessages = current.messagesByConversation.toMutableMap()
                            updatedMessages[conversationId] = combined

                            val updatedHasMore = current.hasMoreByConversation.toMutableMap()
                            updatedHasMore[conversationId] = history.hasMore

                            val updatedOldest = current.oldestMessageIdByConversation.toMutableMap()
                            updatedOldest[conversationId] = history.oldestMessageId

                            current.copy(
                                messagesByConversation = updatedMessages,
                                hasMoreByConversation = updatedHasMore,
                                oldestMessageIdByConversation = updatedOldest,
                                isLoadingOlderMessages = false
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isLoadingOlderMessages = false,
                                snackbarMessage = err.message ?: "Không thể tải tin nhắn cũ hơn."
                            )
                        }
                    }
                )
        }
    }

    fun sendTextMessage(
        conversationId: Long,
        rawText: String,
        onSent: () -> Unit = {}
    ) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return

        val detectedLink = extractFirstLink(trimmed)
        val request = SendMessageRequest(
            content = trimmed,
            attachmentUrl = detectedLink?.url,
            attachmentType = if (detectedLink != null) "LINK" else null,
            attachmentName = detectedLink?.label
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true) }
            repository.sendMessage(conversationId = conversationId, request = request).fold(
                onSuccess = { messageDto ->
                    appendMessageAndBumpConversation(messageDto, isFromActiveScreen = true)
                    _uiState.update { it.copy(isSendingMessage = false) }
                    onSent()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSendingMessage = false,
                            snackbarMessage = err.message ?: "Gửi tin nhắn thất bại."
                        )
                    }
                }
            )
        }
    }

    fun uploadAndSendAttachment(
        conversationId: Long,
        uri: Uri,
        caption: String = "",
        fallbackMime: String? = null,
        onSent: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAttachment = true) }
            repository.uploadFromUri(uri = uri, fallbackMime = fallbackMime).fold(
                onSuccess = { uploaded ->
                    val prefix = when (uploaded.kind.uppercase()) {
                        "IMAGE" -> "[Hình ảnh]"
                        "VIDEO" -> "[Video]"
                        else -> "[Tệp]"
                    }
                    val trimmedCaption = caption.trim()
                    val content = trimmedCaption.ifEmpty { "$prefix ${uploaded.fileName}" }

                    val sendResult = repository.sendMessage(
                        conversationId = conversationId,
                        request = SendMessageRequest(
                            content = content,
                            attachmentUrl = uploaded.url,
                            attachmentType = uploaded.kind,
                            attachmentName = uploaded.fileName,
                            attachmentSize = uploaded.fileSize
                        )
                    )

                    sendResult.fold(
                        onSuccess = { messageDto ->
                            appendMessageAndBumpConversation(messageDto, isFromActiveScreen = true)
                            _uiState.update { it.copy(isUploadingAttachment = false) }
                            onSent()
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isUploadingAttachment = false,
                                    snackbarMessage = err.message ?: "Không thể gửi tệp đính kèm."
                                )
                            }
                        }
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isUploadingAttachment = false,
                            snackbarMessage = err.message ?: "Tải tệp lên máy chủ thất bại."
                        )
                    }
                }
            )
        }
    }

    fun openOrCreatePrivateChat(
        targetUserId: Long,
        fallbackName: String? = null,
        fallbackUsername: String? = null,
        fallbackAvatarUrl: String? = null,
        fallbackOnline: Boolean = false,
        onReady: (Long) -> Unit
    ) {
        val existing = _uiState.value.conversations.firstOrNull {
            !it.isGroup && it.targetUserId == targetUserId
        }
        if (existing != null) {
            onReady(existing.id)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingConversation = true) }
            repository.createPrivateConversation(targetUserId).fold(
                onSuccess = { created ->
                    stompClient.subscribeConversation(created.id)
                    _uiState.update { state ->
                        val alreadyInList = state.conversations.any { it.id == created.id }
                        val updatedList = if (alreadyInList) {
                            state.conversations
                        } else {
                            listOf(
                                ConversationItemResponse(
                                    id = created.id,
                                    name = fallbackName ?: created.name ?: "Hội thoại #${created.id}",
                                    type = "PRIVATE",
                                    avatarUrl = fallbackAvatarUrl,
                                    isOnline = fallbackOnline,
                                    targetUserId = targetUserId,
                                    targetUsername = fallbackUsername
                                )
                            ) + state.conversations
                        }
                        state.copy(
                            conversations = updatedList,
                            isCreatingConversation = false
                        )
                    }
                    refreshConversations()
                    onReady(created.id)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isCreatingConversation = false,
                            snackbarMessage = err.message ?: "Không thể khởi tạo cuộc trò chuyện."
                        )
                    }
                }
            )
        }
    }

    fun createGroupChat(
        name: String,
        memberIds: List<Long>,
        groupVisibility: String = "PUBLIC",
        onReady: (Long) -> Unit
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Vui lòng nhập tên nhóm trò chuyện.") }
            return
        }
        val normalizedVisibility = if (groupVisibility.equals("PRIVATE", ignoreCase = true)) {
            "PRIVATE"
        } else {
            "PUBLIC"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingConversation = true) }
            repository.createGroupConversation(
                name = cleanName,
                memberIds = memberIds,
                groupVisibility = normalizedVisibility
            ).fold(
                onSuccess = { created ->
                    stompClient.subscribeConversation(created.id)
                    val newItem = ConversationItemResponse(
                        id = created.id,
                        name = created.name ?: cleanName,
                        type = "GROUP",
                        groupVisibility = created.groupVisibility ?: normalizedVisibility,
                        inviteCode = created.inviteCode,
                        currentUserRole = "ADMIN",
                        memberCount = memberIds.distinct().size + 1,
                        lastMessage = "Nhóm trò chuyện mới",
                        lastMessageTime = created.createdAt ?: LocalDateTime.now().toString()
                    )
                    _uiState.update { state ->
                        state.copy(
                            conversations = listOf(newItem) + state.conversations.filterNot { it.id == created.id },
                            isCreatingConversation = false,
                            snackbarMessage = "Đã tạo nhóm \"$cleanName\"."
                        )
                    }
                    refreshConversations()
                    onReady(created.id)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isCreatingConversation = false,
                            snackbarMessage = err.message ?: "Không thể tạo nhóm trò chuyện."
                        )
                    }
                }
            )
        }
    }

    fun previewGroupInvite(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    invitePreview = null,
                    invitePreviewError = "Vui lòng nhập liên kết hoặc mã mời nhóm."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingInvitePreview = true,
                    invitePreviewError = null,
                    joinGroupFeedback = null,
                    joinGroupFeedbackIsError = false
                )
            }
            repository.getGroupInvitePreview(trimmed).fold(
                onSuccess = { preview ->
                    _uiState.update {
                        it.copy(
                            invitePreview = preview,
                            isLoadingInvitePreview = false,
                            invitePreviewError = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            invitePreview = null,
                            isLoadingInvitePreview = false,
                            invitePreviewError = err.message ?: "Không tìm thấy nhóm tương ứng với mã mời."
                        )
                    }
                }
            )
        }
    }

    fun joinGroupByInvite(
        rawInput: String,
        onJoinedOpenChat: (Long) -> Unit
    ) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty() || _uiState.value.isJoiningGroupByInvite) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isJoiningGroupByInvite = true,
                    joinGroupFeedback = null,
                    joinGroupFeedbackIsError = false
                )
            }
            repository.joinGroupByInviteCode(trimmed).fold(
                onSuccess = { result ->
                    val statusUpper = result.status.uppercase()
                    if (statusUpper == "JOINED" || statusUpper == "ALREADY_MEMBER") {
                        stompClient.subscribeConversation(result.conversationId)
                        _uiState.update { state ->
                            val exists = state.conversations.any { it.id == result.conversationId }
                            val updatedConvs = if (exists) {
                                state.conversations
                            } else {
                                listOf(
                                    ConversationItemResponse(
                                        id = result.conversationId,
                                        name = result.name ?: "Nhóm #${result.conversationId}",
                                        type = "GROUP",
                                        groupVisibility = result.groupVisibility ?: "PUBLIC",
                                        currentUserRole = "MEMBER",
                                        lastMessage = "Đã tham gia nhóm",
                                        lastMessageTime = LocalDateTime.now().toString()
                                    )
                                ) + state.conversations
                            }
                            state.copy(
                                conversations = updatedConvs,
                                isJoiningGroupByInvite = false,
                                snackbarMessage = result.message
                                    ?: "Đã tham gia nhóm \"${result.name.orEmpty()}\"."
                            )
                        }
                        refreshConversations()
                        clearGroupInviteDialogState()
                        onJoinedOpenChat(result.conversationId)
                    } else {
                        // PENDING_APPROVAL for private group
                        val feedback = result.message
                            ?: "Đã gửi yêu cầu tham gia nhóm riêng tư. Vui lòng chờ Trưởng nhóm hoặc Phó nhóm phê duyệt."
                        _uiState.update { state ->
                            state.copy(
                                isJoiningGroupByInvite = false,
                                invitePreview = state.invitePreview?.copy(joinStatus = "PENDING"),
                                joinGroupFeedback = feedback,
                                joinGroupFeedbackIsError = false,
                                snackbarMessage = feedback
                            )
                        }
                    }
                },
                onFailure = { err ->
                    val msg = err.message ?: "Không thể tham gia nhóm."
                    _uiState.update {
                        it.copy(
                            isJoiningGroupByInvite = false,
                            joinGroupFeedback = msg,
                            joinGroupFeedbackIsError = true
                        )
                    }
                }
            )
        }
    }

    fun clearGroupInviteDialogState() {
        _uiState.update {
            it.copy(
                invitePreview = null,
                isLoadingInvitePreview = false,
                invitePreviewError = null,
                isJoiningGroupByInvite = false,
                joinGroupFeedback = null,
                joinGroupFeedbackIsError = false
            )
        }
    }

    fun updateGroupVisibility(conversationId: Long, groupVisibility: String) {
        if (_uiState.value.isUpdatingGroupSettings) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingGroupSettings = true) }
            repository.updateGroupVisibility(conversationId, groupVisibility).fold(
                onSuccess = { resp ->
                    val nextVis = resp.groupVisibility ?: groupVisibility
                    val label = if (nextVis.equals("PRIVATE", ignoreCase = true)) {
                        "Nhóm riêng tư"
                    } else {
                        "Nhóm công khai"
                    }
                    _uiState.update { state ->
                        val updatedConvs = state.conversations.map { conv ->
                            if (conv.id == conversationId) {
                                conv.copy(
                                    groupVisibility = nextVis,
                                    inviteCode = resp.inviteCode ?: conv.inviteCode
                                )
                            } else {
                                conv
                            }
                        }
                        state.copy(
                            conversations = updatedConvs,
                            isUpdatingGroupSettings = false,
                            snackbarMessage = "Đã chuyển chế độ sang $label."
                        )
                    }
                    if (nextVis.equals("PRIVATE", ignoreCase = true)) {
                        loadGroupJoinRequests(conversationId)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isUpdatingGroupSettings = false,
                            snackbarMessage = err.message ?: "Không thể đổi chế độ hiển thị nhóm."
                        )
                    }
                }
            )
        }
    }

    fun regenerateGroupInviteCode(conversationId: Long) {
        if (_uiState.value.isUpdatingGroupSettings) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingGroupSettings = true) }
            repository.regenerateInviteCode(conversationId).fold(
                onSuccess = { resp ->
                    _uiState.update { state ->
                        val updatedConvs = state.conversations.map { conv ->
                            if (conv.id == conversationId) {
                                conv.copy(
                                    inviteCode = resp.inviteCode,
                                    groupVisibility = resp.groupVisibility ?: conv.groupVisibility
                                )
                            } else {
                                conv
                            }
                        }
                        state.copy(
                            conversations = updatedConvs,
                            isUpdatingGroupSettings = false,
                            snackbarMessage = "Đã làm mới liên kết mời tham gia nhóm."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isUpdatingGroupSettings = false,
                            snackbarMessage = err.message ?: "Không thể làm mới liên kết mời."
                        )
                    }
                }
            )
        }
    }

    fun updateGroupMemberRole(
        conversationId: Long,
        targetMember: GroupMemberDto,
        nextRole: String
    ) {
        if (_uiState.value.processingGroupMemberIds.contains(targetMember.id)) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(processingGroupMemberIds = it.processingGroupMemberIds + targetMember.id)
            }
            repository.updateGroupMemberRole(
                conversationId = conversationId,
                targetUserId = targetMember.id,
                role = nextRole
            ).fold(
                onSuccess = { members ->
                    val notice = if (nextRole.equals("DEPUTY", ignoreCase = true)) {
                        "Đã bổ nhiệm ${targetMember.displayName} làm Phó nhóm."
                    } else {
                        "Đã gỡ quyền Phó nhóm của ${targetMember.displayName}."
                    }
                    applyUpdatedGroupMembers(conversationId, members, notice)
                    _uiState.update {
                        it.copy(processingGroupMemberIds = it.processingGroupMemberIds - targetMember.id)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            processingGroupMemberIds = it.processingGroupMemberIds - targetMember.id,
                            snackbarMessage = err.message ?: "Không thể cập nhật vai trò thành viên."
                        )
                    }
                }
            )
        }
    }

    fun transferGroupLeadership(
        conversationId: Long,
        newLeader: GroupMemberDto
    ) {
        if (_uiState.value.processingGroupMemberIds.contains(newLeader.id)) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(processingGroupMemberIds = it.processingGroupMemberIds + newLeader.id)
            }
            repository.transferGroupLeadership(
                conversationId = conversationId,
                newLeaderUserId = newLeader.id
            ).fold(
                onSuccess = { members ->
                    applyUpdatedGroupMembers(
                        conversationId = conversationId,
                        members = members,
                        snackbar = "Đã chuyển quyền Trưởng nhóm cho ${newLeader.displayName}."
                    )
                    _uiState.update { state ->
                        state.copy(
                            processingGroupMemberIds = state.processingGroupMemberIds - newLeader.id,
                            groupJoinRequestsByConversation = state.groupJoinRequestsByConversation - conversationId
                        )
                    }
                    refreshConversations()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            processingGroupMemberIds = it.processingGroupMemberIds - newLeader.id,
                            snackbarMessage = err.message ?: "Không thể chuyển quyền Trưởng nhóm."
                        )
                    }
                }
            )
        }
    }

    private fun applyUpdatedGroupMembers(
        conversationId: Long,
        members: List<GroupMemberDto>,
        snackbar: String? = null
    ) {
        _uiState.update { state ->
            val currentUsername = state.currentUser?.username
            val myUpdatedRole = members.firstOrNull {
                !currentUsername.isNullOrBlank() && it.username.equals(currentUsername, ignoreCase = true)
            }?.role

            val updatedMembersMap = state.groupMembersByConversation.toMutableMap().apply {
                put(conversationId, members)
            }
            val updatedConvs = state.conversations.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(
                        memberCount = members.size,
                        members = members,
                        currentUserRole = myUpdatedRole ?: conv.currentUserRole
                    )
                } else {
                    conv
                }
            }
            state.copy(
                groupMembersByConversation = updatedMembersMap,
                conversations = updatedConvs,
                snackbarMessage = snackbar ?: state.snackbarMessage
            )
        }
    }

    fun loadGroupJoinRequests(conversationId: Long) {
        viewModelScope.launch {
            repository.getGroupJoinRequests(conversationId).onSuccess { requests ->
                _uiState.update { state ->
                    val updatedReqMap = state.groupJoinRequestsByConversation.toMutableMap().apply {
                        put(conversationId, requests)
                    }
                    val updatedConvs = state.conversations.map { conv ->
                        if (conv.id == conversationId) {
                            conv.copy(pendingJoinRequestCount = requests.size)
                        } else {
                            conv
                        }
                    }
                    state.copy(
                        groupJoinRequestsByConversation = updatedReqMap,
                        conversations = updatedConvs
                    )
                }
            }
        }
    }

    fun approveGroupJoinRequest(
        conversationId: Long,
        request: GroupJoinRequestDto
    ) {
        if (_uiState.value.processingJoinRequestIds.contains(request.id)) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(processingJoinRequestIds = it.processingJoinRequestIds + request.id)
            }
            repository.approveGroupJoinRequest(conversationId, request.id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val remaining = state.groupJoinRequestsByConversation[conversationId]
                            .orEmpty()
                            .filterNot { it.id == request.id }
                        val updatedMap = state.groupJoinRequestsByConversation.toMutableMap().apply {
                            put(conversationId, remaining)
                        }
                        val updatedConvs = state.conversations.map { conv ->
                            if (conv.id == conversationId) {
                                conv.copy(pendingJoinRequestCount = remaining.size)
                            } else {
                                conv
                            }
                        }
                        state.copy(
                            processingJoinRequestIds = state.processingJoinRequestIds - request.id,
                            groupJoinRequestsByConversation = updatedMap,
                            conversations = updatedConvs,
                            snackbarMessage = "Đã duyệt ${request.displayName} vào nhóm."
                        )
                    }
                    loadConversationMembers(conversationId)
                    refreshConversations()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            processingJoinRequestIds = it.processingJoinRequestIds - request.id,
                            snackbarMessage = err.message ?: "Không thể duyệt yêu cầu tham gia."
                        )
                    }
                }
            )
        }
    }

    fun rejectGroupJoinRequest(
        conversationId: Long,
        request: GroupJoinRequestDto
    ) {
        if (_uiState.value.processingJoinRequestIds.contains(request.id)) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(processingJoinRequestIds = it.processingJoinRequestIds + request.id)
            }
            repository.rejectGroupJoinRequest(conversationId, request.id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val remaining = state.groupJoinRequestsByConversation[conversationId]
                            .orEmpty()
                            .filterNot { it.id == request.id }
                        val updatedMap = state.groupJoinRequestsByConversation.toMutableMap().apply {
                            put(conversationId, remaining)
                        }
                        val updatedConvs = state.conversations.map { conv ->
                            if (conv.id == conversationId) {
                                conv.copy(pendingJoinRequestCount = remaining.size)
                            } else {
                                conv
                            }
                        }
                        state.copy(
                            processingJoinRequestIds = state.processingJoinRequestIds - request.id,
                            groupJoinRequestsByConversation = updatedMap,
                            conversations = updatedConvs,
                            snackbarMessage = "Đã từ chối yêu cầu của ${request.displayName}."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            processingJoinRequestIds = it.processingJoinRequestIds - request.id,
                            snackbarMessage = err.message ?: "Không thể từ chối yêu cầu tham gia."
                        )
                    }
                }
            )
        }
    }

    fun onUserSearchKeywordChange(keyword: String) {
        val normalized = keyword.trim().trimStart('@')
        searchDebounceJob?.cancel()
        if (normalized.isEmpty()) {
            _uiState.update {
                it.copy(
                    userSearchKeyword = keyword,
                    hasSearchedUsers = false,
                    isSearchingUsers = false,
                    userSearchResults = emptyList()
                )
            }
            return
        }

        _uiState.update { it.copy(userSearchKeyword = keyword, isSearchingUsers = true) }
        searchDebounceJob = viewModelScope.launch {
            delay(320L)
            executeUserSearch(keyword)
        }
    }

    fun executeUserSearch(keyword: String = _uiState.value.userSearchKeyword) {
        val normalized = keyword.trim().trimStart('@')
        if (normalized.isEmpty()) {
            _uiState.update {
                it.copy(
                    hasSearchedUsers = false,
                    isSearchingUsers = false,
                    userSearchResults = emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUsers = true, hasSearchedUsers = true) }
            repository.searchUsers(normalized).fold(
                onSuccess = { users ->
                    _uiState.update { state ->
                        val clearedIds = users
                            .filter {
                                it.isFriend != true &&
                                    !it.friendshipStatus.equals("PENDING", ignoreCase = true)
                            }
                            .map { it.id }
                            .toSet()
                        val mergedKnown = state.knownUsersById.toMutableMap()
                        users.forEach { u -> mergedKnown[u.id] = u }
                        state.copy(
                            isSearchingUsers = false,
                            hasSearchedUsers = true,
                            userSearchResults = users,
                            knownUsersById = mergedKnown,
                            sentRequests = if (clearedIds.isEmpty()) {
                                state.sentRequests
                            } else {
                                state.sentRequests.filterNot { it.addresseeId in clearedIds }
                            },
                            receivedRequests = if (clearedIds.isEmpty()) {
                                state.receivedRequests
                            } else {
                                state.receivedRequests.filterNot { it.requesterId in clearedIds }
                            }
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSearchingUsers = false,
                            hasSearchedUsers = true,
                            userSearchResults = emptyList(),
                            snackbarMessage = err.message ?: "Không thể tìm kiếm người dùng."
                        )
                    }
                }
            )
        }
    }

    fun resetUserSearch() {
        searchDebounceJob?.cancel()
        _uiState.update { state ->
            state.copy(
                userSearchKeyword = "",
                hasSearchedUsers = false,
                isSearchingUsers = false,
                userSearchResults = emptyList(),
                addFriendFeedback = null,
                addFriendFeedbackIsError = false
            )
        }
    }

    fun resolveUserRelationship(
        user: UserSearchResponse,
        state: MainChatUiState = _uiState.value
    ): UserRelationshipState {
        if (state.currentUser?.id == user.id) return UserRelationshipState.SELF
        if (
            user.isFriend == true ||
            user.friendshipStatus.equals("ACCEPTED", ignoreCase = true) ||
            state.friends.any { it.id == user.id }
        ) {
            return UserRelationshipState.FRIEND
        }
        if (state.receivedRequests.any { it.requesterId == user.id && it.status == "PENDING" }) {
            return UserRelationshipState.RECEIVED_PENDING
        }
        if (
            user.friendshipStatus.equals("PENDING", ignoreCase = true) ||
            state.sentRequests.any { it.addresseeId == user.id && it.status == "PENDING" }
        ) {
            return UserRelationshipState.SENT_PENDING
        }
        return UserRelationshipState.NONE
    }

    fun sendFriendRequest(user: UserSearchResponse) {
        if (_uiState.value.sendingFriendRequestUserIds.contains(user.id)) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    sendingFriendRequestUserIds = state.sendingFriendRequestUserIds + user.id,
                    addFriendFeedback = null,
                    addFriendFeedbackIsError = false
                )
            }
            repository.sendFriendRequest(user.id).fold(
                onSuccess = { friendship ->
                    _uiState.update { state ->
                        val updatedKnown = state.knownUsersById.toMutableMap()
                        updatedKnown[user.id] = user.copy(friendshipStatus = "PENDING")
                        val updatedResults = state.userSearchResults.map { item ->
                            if (item.id == user.id) item.copy(friendshipStatus = "PENDING") else item
                        }
                        val msg = "Đã gửi lời mời kết bạn tới ${user.fullName ?: user.username}."
                        state.copy(
                            sendingFriendRequestUserIds = state.sendingFriendRequestUserIds - user.id,
                            sentRequests = listOf(friendship) + state.sentRequests.filterNot { it.id == friendship.id },
                            userSearchResults = updatedResults,
                            knownUsersById = updatedKnown,
                            addFriendFeedback = msg,
                            addFriendFeedbackIsError = false,
                            snackbarMessage = msg
                        )
                    }
                },
                onFailure = { err ->
                    val errMsg = err.message ?: "Không thể gửi lời mời kết bạn."
                    _uiState.update { state ->
                        state.copy(
                            sendingFriendRequestUserIds = state.sendingFriendRequestUserIds - user.id,
                            addFriendFeedback = errMsg,
                            addFriendFeedbackIsError = true,
                            snackbarMessage = errMsg
                        )
                    }
                }
            )
        }
    }

    fun acceptFriendRequest(request: FriendshipResponse) {
        if (_uiState.value.processingRequestIds.contains(request.id)) return

        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestIds = it.processingRequestIds + request.id) }
            repository.acceptFriendRequest(request.id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            processingRequestIds = state.processingRequestIds - request.id,
                            receivedRequests = state.receivedRequests.filterNot { it.id == request.id },
                            snackbarMessage = "Đã chấp nhận lời mời kết bạn."
                        )
                    }
                    refreshFriendsAndRequests()
                    refreshConversations()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            processingRequestIds = it.processingRequestIds - request.id,
                            snackbarMessage = err.message ?: "Không thể chấp nhận lời mời."
                        )
                    }
                }
            )
        }
    }

    fun rejectFriendRequest(request: FriendshipResponse) {
        if (_uiState.value.processingRequestIds.contains(request.id)) return

        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestIds = it.processingRequestIds + request.id) }
            repository.rejectFriendRequest(request.id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            processingRequestIds = state.processingRequestIds - request.id,
                            receivedRequests = state.receivedRequests.filterNot { it.id == request.id },
                            snackbarMessage = "Đã từ chối lời mời kết bạn."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            processingRequestIds = it.processingRequestIds - request.id,
                            snackbarMessage = err.message ?: "Không thể từ chối lời mời."
                        )
                    }
                }
            )
        }
    }

    fun cancelSentFriendRequest(request: FriendshipResponse) {
        if (_uiState.value.processingRequestIds.contains(request.id)) return

        viewModelScope.launch {
            _uiState.update { it.copy(processingRequestIds = it.processingRequestIds + request.id) }
            repository.cancelSentFriendRequest(request.id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val targetId = request.addresseeId
                        val updatedKnown = state.knownUsersById.toMutableMap()
                        updatedKnown[targetId]?.let { existing ->
                            updatedKnown[targetId] = existing.copy(friendshipStatus = null)
                        }
                        val updatedSearch = state.userSearchResults.map { item ->
                            if (item.id == targetId) item.copy(friendshipStatus = null) else item
                        }
                        val updatedConvs = state.conversations.map { conv ->
                            if (!conv.isGroup && conv.targetUserId == targetId) {
                                conv.copy(friendshipStatus = null)
                            } else {
                                conv
                            }
                        }
                        state.copy(
                            processingRequestIds = state.processingRequestIds - request.id,
                            sentRequests = state.sentRequests.filterNot { it.id == request.id },
                            knownUsersById = updatedKnown,
                            userSearchResults = updatedSearch,
                            conversations = updatedConvs,
                            snackbarMessage = "Đã thu hồi lời mời kết bạn."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            processingRequestIds = it.processingRequestIds - request.id,
                            snackbarMessage = err.message ?: "Không thể thu hồi lời mời."
                        )
                    }
                }
            )
        }
    }

    fun loadConversationMembers(conversationId: Long) {
        viewModelScope.launch {
            repository.getConversationMembers(conversationId).onSuccess { members ->
                applyUpdatedGroupMembers(conversationId = conversationId, members = members)
                val conv = _uiState.value.conversations.firstOrNull { it.id == conversationId }
                if (conv?.canManageGroupInvitesAndRequests == true) {
                    loadGroupJoinRequests(conversationId)
                }
            }
        }
    }

    fun unfriendUser(
        targetUserId: Long,
        displayName: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.unfriend(targetUserId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val updatedKnown = state.knownUsersById.toMutableMap()
                        updatedKnown[targetUserId]?.let { existing ->
                            updatedKnown[targetUserId] = existing.copy(
                                isFriend = false,
                                friendshipStatus = null
                            )
                        }

                        val updatedSearchResults = state.userSearchResults.map { item ->
                            if (item.id == targetUserId) {
                                item.copy(isFriend = false, friendshipStatus = null)
                            } else {
                                item
                            }
                        }

                        val updatedConvs = state.conversations.map { conv ->
                            if (!conv.isGroup && conv.targetUserId == targetUserId) {
                                conv.copy(isFriend = false, friendshipStatus = null)
                            } else {
                                conv
                            }
                        }

                        state.copy(
                            friends = state.friends.filterNot { it.id == targetUserId },
                            receivedRequests = state.receivedRequests.filterNot { it.requesterId == targetUserId },
                            sentRequests = state.sentRequests.filterNot { it.addresseeId == targetUserId },
                            knownUsersById = updatedKnown,
                            userSearchResults = updatedSearchResults,
                            conversations = updatedConvs,
                            snackbarMessage = "Đã hủy kết bạn với $displayName."
                        )
                    }
                    onDone()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(snackbarMessage = err.message ?: "Không thể hủy kết bạn.")
                    }
                }
            )
        }
    }

    fun sendFriendRequestById(targetUserId: Long, displayName: String) {
        if (_uiState.value.sendingFriendRequestUserIds.contains(targetUserId)) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(sendingFriendRequestUserIds = it.sendingFriendRequestUserIds + targetUserId)
            }
            repository.sendFriendRequest(targetUserId).fold(
                onSuccess = { friendship ->
                    _uiState.update { state ->
                        val updatedConvs = state.conversations.map { conv ->
                            if (!conv.isGroup && conv.targetUserId == targetUserId) {
                                conv.copy(friendshipStatus = "PENDING")
                            } else {
                                conv
                            }
                        }
                        state.copy(
                            sendingFriendRequestUserIds = state.sendingFriendRequestUserIds - targetUserId,
                            sentRequests = listOf(friendship) + state.sentRequests.filterNot { it.id == friendship.id },
                            conversations = updatedConvs,
                            snackbarMessage = "Đã gửi lời mời kết bạn tới $displayName."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            sendingFriendRequestUserIds = it.sendingFriendRequestUserIds - targetUserId,
                            snackbarMessage = err.message ?: "Không thể gửi lời mời kết bạn."
                        )
                    }
                }
            )
        }
    }

    fun updateProfile(
        fullName: String,
        email: String,
        phone: String,
        gender: String,
        dob: String,
        bio: String,
        avatarUrl: String?,
        onSuccess: () -> Unit = {}
    ) {
        val cleanName = fullName.trim()
        val cleanDob = dob.trim()
        if (cleanName.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Họ tên hiển thị không được để trống.") }
            return
        }
        if (cleanDob.isNotEmpty() && !Regex("""^\d{4}-\d{2}-\d{2}$""").matches(cleanDob)) {
            _uiState.update { it.copy(snackbarMessage = "Ngày sinh cần nhập đúng định dạng YYYY-MM-DD.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true) }
            val request = UpdateProfileRequest(
                fullName = cleanName,
                email = email.trim(),
                phone = phone.trim(),
                gender = gender.trim().ifEmpty { null },
                dob = cleanDob.ifEmpty { null },
                bio = bio.trim(),
                avatarUrl = avatarUrl
            )
            repository.updateMyProfile(request).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            profile = updated,
                            isSavingProfile = false,
                            snackbarMessage = "Đã cập nhật hồ sơ cá nhân."
                        )
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            snackbarMessage = err.message ?: "Cập nhật hồ sơ thất bại."
                        )
                    }
                }
            )
        }
    }

    fun uploadAvatarAndSave(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true) }
            repository.uploadFromUri(uri = uri, fallbackMime = "image/jpeg").fold(
                onSuccess = { uploaded ->
                    val current = _uiState.value.profile
                    val fallbackUser = _uiState.value.currentUser
                    val updateRequest = UpdateProfileRequest(
                        fullName = current?.fullName ?: fallbackUser?.fullName,
                        email = current?.email ?: fallbackUser?.email,
                        phone = current?.phone ?: fallbackUser?.phone,
                        gender = current?.gender ?: fallbackUser?.gender,
                        dob = current?.dob ?: fallbackUser?.dob,
                        bio = current?.bio ?: fallbackUser?.bio,
                        avatarUrl = uploaded.url
                    )
                    repository.updateMyProfile(updateRequest).fold(
                        onSuccess = { updatedProfile ->
                            _uiState.update {
                                it.copy(
                                    profile = updatedProfile,
                                    isUploadingAvatar = false,
                                    snackbarMessage = "Đã cập nhật ảnh đại diện mới."
                                )
                            }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    isUploadingAvatar = false,
                                    snackbarMessage = err.message ?: "Không thể lưu ảnh đại diện."
                                )
                            }
                        }
                    )
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            snackbarMessage = err.message ?: "Tải ảnh đại diện thất bại."
                        )
                    }
                }
            )
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            sessionPreferences.setDarkMode(enabled)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            sessionPreferences.setDynamicColor(enabled)
        }
    }

    fun setPresenceMode(mode: UserPresenceMode) {
        viewModelScope.launch {
            sessionPreferences.setPresenceMode(mode)
            _uiState.update { it.copy(presenceMode = mode) }
            val username = _uiState.value.currentUser?.username ?: initializedForUsername
            if (!username.isNullOrBlank()) {
                stompClient.connect(username)
            }
            repository.updateMyPresence(mode.rawValue).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            profile = updated,
                            snackbarMessage = "Đã chuyển trạng thái sang ${mode.title}."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(snackbarMessage = err.message ?: "Không thể cập nhật trạng thái.")
                    }
                }
            )
        }
    }

    fun updateServerUrl(newUrl: String) {
        viewModelScope.launch {
            val normalized = sessionPreferences.setServerBaseUrl(newUrl)
            stompClient.reconnectWithLatestEndpoint()
            _uiState.update {
                it.copy(
                    serverBaseUrl = normalized,
                    snackbarMessage = "Đã chuyển máy chủ sang $normalized"
                )
            }
            refreshAll()
        }
    }

    fun resolveAssetUrl(rawUrl: String?): String? {
        return repository.resolveAssetUrl(rawUrl, _uiState.value.serverBaseUrl)
    }

    fun deleteConversation(
        conversationId: Long,
        customNotice: String? = null,
        onDeleted: () -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId).fold(
                onSuccess = {
                    val targetConv = _uiState.value.conversations.firstOrNull { it.id == conversationId }
                    val defaultNotice = when {
                        customNotice != null -> customNotice
                        targetConv?.isGroup == true && targetConv.isCurrentUserGroupAdmin ->
                            "Đã giải tán nhóm trò chuyện."
                        targetConv?.isGroup == true ->
                            "Đã rời khỏi nhóm trò chuyện."
                        else -> "Đã xóa cuộc trò chuyện."
                    }
                    val remaining = _uiState.value.conversations.filterNot { it.id == conversationId }
                    stompClient.syncConversationSubscriptions(remaining.map { it.id })
                    _uiState.update { state ->
                        state.copy(
                            conversations = remaining,
                            messagesByConversation = state.messagesByConversation - conversationId,
                            groupMembersByConversation = state.groupMembersByConversation - conversationId,
                            groupJoinRequestsByConversation = state.groupJoinRequestsByConversation - conversationId,
                            hasMoreByConversation = state.hasMoreByConversation - conversationId,
                            oldestMessageIdByConversation = state.oldestMessageIdByConversation - conversationId,
                            unreadCounts = state.unreadCounts - conversationId,
                            activeConversationId = if (state.activeConversationId == conversationId) null else state.activeConversationId,
                            snackbarMessage = defaultNotice
                        )
                    }
                    onDeleted()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(snackbarMessage = err.message ?: "Không thể thực hiện thao tác.")
                    }
                }
            )
        }
    }

    fun deleteMessages(
        conversationId: Long,
        messageIds: List<Long>,
        mode: String,
        onSuccess: () -> Unit = {}
    ) {
        val uniqueIds = messageIds.distinct().filter { it > 0L }
        if (uniqueIds.isEmpty() || _uiState.value.isDeletingMessages) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingMessages = true) }
            repository.deleteMessages(
                conversationId = conversationId,
                messageIds = uniqueIds,
                mode = mode
            ).fold(
                onSuccess = { response ->
                    applyMessageDeletion(response)
                    val deletedCount = response.deletedIds.size.coerceAtLeast(uniqueIds.size)
                    val notice = if (mode.equals("FOR_EVERYONE", ignoreCase = true)) {
                        "Đã thu hồi $deletedCount tin nhắn."
                    } else {
                        "Đã xóa $deletedCount tin nhắn ở phía bạn."
                    }
                    _uiState.update {
                        it.copy(
                            isDeletingMessages = false,
                            snackbarMessage = notice
                        )
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isDeletingMessages = false,
                            snackbarMessage = err.message ?: "Không thể xóa tin nhắn."
                        )
                    }
                }
            )
        }
    }

    private fun applyMessageDeletion(event: DeleteMessagesResponse) {
        val deletedSet = event.deletedIds.toHashSet()
        if (deletedSet.isEmpty()) return

        _uiState.update { state ->
            val currentMessages = state.messagesByConversation[event.conversationId]
            val updatedMessagesMap = if (currentMessages != null) {
                val filtered = currentMessages.filterNot { it.id in deletedSet }
                state.messagesByConversation.toMutableMap().apply {
                    put(event.conversationId, filtered)
                }
            } else {
                state.messagesByConversation
            }

            val updatedOldestMap = if (currentMessages != null) {
                val filtered = updatedMessagesMap[event.conversationId].orEmpty()
                state.oldestMessageIdByConversation.toMutableMap().apply {
                    put(event.conversationId, filtered.firstOrNull()?.id)
                }
            } else {
                state.oldestMessageIdByConversation
            }

            val updatedConversations = state.conversations.map { conv ->
                if (conv.id == event.conversationId) {
                    conv.copy(
                        lastMessage = event.lastMessage,
                        lastMessageSenderName = event.lastMessageSenderName,
                        lastMessageSenderUsername = event.lastMessageSenderUsername,
                        lastMessageTime = event.lastMessageTime
                    )
                } else {
                    conv
                }
            }

            state.copy(
                messagesByConversation = updatedMessagesMap,
                oldestMessageIdByConversation = updatedOldestMap,
                conversations = updatedConversations
            )
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            stompClient.disconnect()
            repository.logout()
            _uiState.value = MainChatUiState(serverBaseUrl = sessionPreferences.currentBaseUrl)
            onComplete()
        }
    }

    private fun handleIncomingRealtimeMessage(dto: MessageDto) {
        val state = _uiState.value
        val conversationExists = state.conversations.any { it.id == dto.conversationId }
        if (!conversationExists) {
            refreshConversations()
        }
        val isActive = state.activeConversationId == dto.conversationId
        appendMessageAndBumpConversation(dto, isFromActiveScreen = isActive)
    }

    private fun appendMessageAndBumpConversation(
        dto: MessageDto,
        isFromActiveScreen: Boolean
    ) {
        _uiState.update { state ->
            val currentList = state.messagesByConversation[dto.conversationId].orEmpty()
            val alreadyExists = currentList.any { it.id == dto.id }
            val updatedMessagesMap = if (alreadyExists) {
                state.messagesByConversation
            } else {
                state.messagesByConversation.toMutableMap().apply {
                    put(dto.conversationId, currentList + dto)
                }
            }

            val targetConv = state.conversations.firstOrNull { it.id == dto.conversationId }
            val updatedConversations = if (targetConv != null) {
                val updatedConv = targetConv.copy(
                    lastMessage = dto.content,
                    lastMessageSenderName = dto.sender.fullName ?: dto.sender.username,
                    lastMessageSenderUsername = dto.sender.username,
                    lastMessageTime = dto.createdAt ?: LocalDateTime.now().toString()
                )
                listOf(updatedConv) + state.conversations.filterNot { it.id == dto.conversationId }
            } else {
                state.conversations
            }

            val isFromMe = dto.sender.username.equals(
                state.currentUser?.username,
                ignoreCase = true
            )
            val updatedUnread = if (!isFromActiveScreen && !isFromMe && !alreadyExists) {
                state.unreadCounts.toMutableMap().apply {
                    put(dto.conversationId, (get(dto.conversationId) ?: 0) + 1)
                }
            } else {
                state.unreadCounts
            }

            state.copy(
                messagesByConversation = updatedMessagesMap,
                conversations = updatedConversations,
                unreadCounts = updatedUnread
            )
        }
    }

    private fun handlePresenceEvent(event: PresenceEvent) {
        val resolvedStatus = event.presenceStatus ?: if (event.isOnline) "ONLINE" else "OFFLINE"
        val isSelf = event.username.equals(_uiState.value.currentUser?.username, ignoreCase = true)
        if (isSelf && event.isOnline) {
            val syncedMode = UserPresenceMode.fromRaw(resolvedStatus)
            viewModelScope.launch {
                sessionPreferences.setPresenceMode(syncedMode)
            }
        }

        _uiState.update { state ->
            val updatedConversations = state.conversations.map { conv ->
                if (!conv.isGroup && conv.targetUserId == event.userId) {
                    conv.copy(
                        isOnline = event.isOnline,
                        presenceStatus = resolvedStatus,
                        lastSeen = event.lastSeen ?: conv.lastSeen
                    )
                } else {
                    conv
                }
            }

            val updatedFriends = state.friends.map { friend ->
                if (friend.id == event.userId) {
                    friend.copy(
                        isOnline = event.isOnline,
                        presenceStatus = resolvedStatus,
                        lastSeen = event.lastSeen ?: friend.lastSeen
                    )
                } else {
                    friend
                }
            }

            val updatedGroupMembers = state.groupMembersByConversation.mapValues { (_, list) ->
                list.map { member ->
                    if (member.id == event.userId) {
                        member.copy(
                            isOnline = event.isOnline,
                            presenceStatus = resolvedStatus,
                            lastSeen = event.lastSeen ?: member.lastSeen
                        )
                    } else {
                        member
                    }
                }
            }

            state.copy(
                presenceMode = if (isSelf && event.isOnline) UserPresenceMode.fromRaw(resolvedStatus) else state.presenceMode,
                conversations = updatedConversations,
                friends = updatedFriends,
                groupMembersByConversation = updatedGroupMembers
            )
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainChatViewModel(container) as T
                }
            }
    }
}
