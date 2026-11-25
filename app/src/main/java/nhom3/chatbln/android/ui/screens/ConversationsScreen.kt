package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.local.UserPresenceMode
import nhom3.chatbln.android.data.model.ConversationItemResponse
import nhom3.chatbln.android.data.model.FriendResponse
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.theme.OnlineIndicatorDark
import nhom3.chatbln.android.ui.theme.OnlineIndicatorLight
import nhom3.chatbln.android.ui.util.formatConversationListTime
import nhom3.chatbln.android.ui.viewmodel.MainChatUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    uiState: MainChatUiState,
    resolveAssetUrl: (String?) -> String?,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onChangePresenceMode: (UserPresenceMode) -> Unit,
    onConversationClick: (Long) -> Unit,
    onStartPrivateChat: (FriendResponse) -> Unit,
    onCreateGroupChat: (name: String, memberIds: List<Long>) -> Unit
) {
    var showNewChatSheet by rememberSaveable { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val connectedDotColor = if (isDarkSurface) OnlineIndicatorDark else OnlineIndicatorLight
    val statusDotColor = when {
        uiState.presenceMode == UserPresenceMode.OFFLINE -> MaterialTheme.colorScheme.outline
        !uiState.isWsConnected -> Color(0xFFF59E0B)
        uiState.presenceMode == UserPresenceMode.ONLINE -> connectedDotColor
        uiState.presenceMode == UserPresenceMode.AWAY -> Color(0xFFEAB308)
        uiState.presenceMode == UserPresenceMode.BUSY -> Color(0xFFEF4444)
        else -> connectedDotColor
    }
    val statusLabel = when {
        uiState.presenceMode == UserPresenceMode.OFFLINE -> "Ẩn trạng thái"
        !uiState.isWsConnected -> "${uiState.presenceMode.title} · Đang nối..."
        else -> uiState.presenceMode.title
    }

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
            onCreateGroup = { groupName, memberIds ->
                showNewChatSheet = false
                onCreateGroupChat(groupName, memberIds)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Đoạn chat",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { showStatusMenu = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(statusDotColor)
                                )
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Outlined.UnfoldMore,
                                    contentDescription = null,
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
                                DropdownMenuItem(
                                    text = { Text(mode.title) },
                                    trailingIcon = {
                                        if (uiState.presenceMode == mode) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null
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

                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Làm mới"
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
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Xóa tìm kiếm"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                uiState.isConversationsLoading && uiState.conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                !uiState.conversationsError.isNullOrBlank() && uiState.conversations.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.conversationsError,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRefresh) {
                            Text("Thử lại")
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
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.conversationSearchQuery.isBlank()) {
                                "Chưa có cuộc trò chuyện nào"
                            } else {
                                "Không tìm thấy cuộc trò chuyện phù hợp"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.conversationSearchQuery.isBlank()) {
                                "Chọn bạn bè hoặc tạo nhóm mới để bắt đầu nhắn tin."
                            } else {
                                "Hãy thử từ khóa khác."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.conversationSearchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { showNewChatSheet = true }) {
                                Text("Bắt đầu trò chuyện ngay")
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
                            key = { it.id }
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
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

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
