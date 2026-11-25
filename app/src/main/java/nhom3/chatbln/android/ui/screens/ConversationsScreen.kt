package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.local.UserPresenceMode
import nhom3.chatbln.android.data.model.ConversationItemResponse
import nhom3.chatbln.android.data.model.FriendResponse
import nhom3.chatbln.android.ui.components.AlertBannerTone
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.components.InlineAlertBanner
import nhom3.chatbln.android.ui.components.TonalListSkeleton
import nhom3.chatbln.android.ui.components.resolvePresenceVisual
import nhom3.chatbln.android.ui.util.formatConversationListTime
import nhom3.chatbln.android.ui.viewmodel.MainChatUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationsScreen(
    uiState: MainChatUiState,
    resolveAssetUrl: (String?) -> String?,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onChangePresenceMode: (UserPresenceMode) -> Unit,
    onConversationClick: (Long) -> Unit,
    onStartPrivateChat: (FriendResponse) -> Unit,
    onCreateGroupChat: (name: String, memberIds: List<Long>, groupVisibility: String) -> Unit,
    onPreviewGroupInvite: (String) -> Unit,
    onJoinGroupByInvite: (String) -> Unit,
    onClearInviteDialog: () -> Unit
) {
    var showNewChatSheet by rememberSaveable { mutableStateOf(false) }
    var showJoinByLinkSheet by rememberSaveable { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    val presenceVisual = resolvePresenceVisual(
        mode = uiState.presenceMode,
        isWsConnected = uiState.isWsConnected
    )

    val filteredConversations = remember(
        uiState.conversations,
        uiState.conversationSearchQuery
    ) {
        val keyword = uiState.conversationSearchQuery.trim()
        if (keyword.isEmpty()) {
            uiState.conversations
        } else {
            uiState.conversations.filter { item ->
                (item.name.orEmpty()).contains(keyword, ignoreCase = true) ||
                    (item.targetUsername.orEmpty()).contains(keyword, ignoreCase = true) ||
                    (item.lastMessage.orEmpty()).contains(keyword, ignoreCase = true)
            }
        }
    }

    if (showNewChatSheet) {
        NewChatBottomSheet(
            friends = uiState.friends,
            isSubmitting = uiState.isCreatingConversation,
            resolveAssetUrl = resolveAssetUrl,
            onDismiss = { showNewChatSheet = false },
            onSelectFriendForPrivateChat = { friend ->
                showNewChatSheet = false
                onStartPrivateChat(friend)
            },
            onCreateGroup = { groupName, memberIds, groupVisibility ->
                showNewChatSheet = false
                onCreateGroupChat(groupName, memberIds, groupVisibility)
            },
            onOpenJoinByInvite = {
                showNewChatSheet = false
                onClearInviteDialog()
                showJoinByLinkSheet = true
            }
        )
    }

    if (showJoinByLinkSheet) {
        JoinGroupBottomSheet(
            preview = uiState.invitePreview,
            isLoadingPreview = uiState.isLoadingInvitePreview,
            previewError = uiState.invitePreviewError,
            isJoining = uiState.isJoiningGroupByInvite,
            feedbackMessage = uiState.joinGroupFeedback,
            feedbackIsError = uiState.joinGroupFeedbackIsError,
            onPreview = onPreviewGroupInvite,
            onJoin = onJoinGroupByInvite,
            onOpenExistingGroup = { convId ->
                showJoinByLinkSheet = false
                onClearInviteDialog()
                onConversationClick(convId)
            },
            onDismiss = {
                showJoinByLinkSheet = false
                onClearInviteDialog()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Đoạn chat",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                actions = {
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable { showStatusMenu = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = presenceVisual.icon,
                                    contentDescription = null,
                                    tint = presenceVisual.color,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = presenceVisual.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Outlined.UnfoldMore,
                                    contentDescription = "Chọn trạng thái hoạt động",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            UserPresenceMode.entries.forEach { mode ->
                                val itemSpec = resolvePresenceVisual(mode = mode, isWsConnected = true)
                                DropdownMenuItem(
                                    text = { Text(mode.title) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = itemSpec.icon,
                                            contentDescription = null,
                                            tint = itemSpec.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        if (uiState.presenceMode == mode) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = "Đang chọn"
                                            )
                                        }
                                    },
                                    onClick = {
                                        showStatusMenu = false
                                        onChangePresenceMode(mode)
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            onClearInviteDialog()
                            showJoinByLinkSheet = true
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GroupAdd,
                            contentDescription = "Tham gia nhóm bằng link hoặc mã mời"
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Làm mới danh sách đoạn chat"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewChatSheet = true },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null
                    )
                },
                text = { Text("Tin nhắn mới") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = uiState.conversationSearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Tìm cuộc trò chuyện hoặc tin nhắn...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.conversationSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Xóa từ khóa tìm kiếm"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                uiState.isConversationsLoading && uiState.conversations.isEmpty() -> {
                    TonalListSkeleton(rowCount = 7)
                }

                !uiState.conversationsError.isNullOrBlank() && uiState.conversations.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InlineAlertBanner(
                            message = uiState.conversationsError,
                            tone = AlertBannerTone.ERROR
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Thử tải lại")
                        }
                    }
                }

                filteredConversations.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.conversationSearchQuery.isBlank()) {
                                "Chưa có cuộc trò chuyện nào"
                            } else {
                                "Không tìm thấy cuộc trò chuyện phù hợp"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.conversationSearchQuery.isBlank()) {
                                "Bắt đầu trò chuyện mới với bạn bè, tạo nhóm hoặc tham gia nhóm bằng mã mời."
                            } else {
                                "Hãy kiểm tra lại từ khóa tìm kiếm."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.conversationSearchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showNewChatSheet = true },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Bắt đầu trò chuyện")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onClearInviteDialog()
                                        showJoinByLinkSheet = true
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Tham gia bằng mã mời")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = filteredConversations,
                            key = { it.id },
                            contentType = {
                                if (it.isGroup) "group_conversation" else "private_conversation"
                            }
                        ) { conversation ->
                            val unread = uiState.unreadCounts[conversation.id] ?: 0
                            ConversationRow(
                                item = conversation,
                                currentUsername = uiState.currentUser?.username,
                                unreadCount = unread,
                                resolvedAvatarUrl = resolveAssetUrl(conversation.avatarUrl),
                                onClick = { onConversationClick(conversation.id) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    item: ConversationItemResponse,
    currentUsername: String?,
    unreadCount: Int,
    resolvedAvatarUrl: String?,
    onClick: () -> Unit
) {
    val title = item.name?.takeIf { it.isNotBlank() } ?: "Hội thoại #${item.id}"
    val isLastFromMe = !currentUsername.isNullOrBlank() &&
        item.lastMessageSenderUsername.equals(currentUsername, ignoreCase = true)

    val previewText = when {
        item.lastMessage.isNullOrBlank() -> {
            if (item.isGroup) "Nhóm trò chuyện mới" else "Bắt đầu cuộc trò chuyện"
        }
        isLastFromMe -> "Bạn: ${item.lastMessage}"
        item.isGroup && !item.lastMessageSenderName.isNullOrBlank() -> {
            "${item.lastMessageSenderName}: ${item.lastMessage}"
        }
        else -> item.lastMessage
    }

    val timeLabel = formatConversationListTime(item.lastMessageTime)
    val pendingApprovals = if (item.isGroup && item.canManageGroupInvitesAndRequests) {
        item.pendingJoinRequestCount ?: 0
    } else {
        0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(
            displayName = title,
            avatarUrl = resolvedAvatarUrl,
            size = 50.dp,
            isOnline = item.isOnline == true,
            presenceStatus = item.presenceStatus,
            showOnlineBadge = !item.isGroup,
            isGroup = item.isGroup
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (item.isGroup && item.isPrivateGroup) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Nhóm riêng tư",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (timeLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unreadCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    color = if (unreadCount > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (pendingApprovals > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "$pendingApprovals chờ duyệt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge {
                        Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                    }
                }
            }
        }
    }
}
