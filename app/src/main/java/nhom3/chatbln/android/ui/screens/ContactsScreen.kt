package nhom3.chatbln.android.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.model.FriendResponse
import nhom3.chatbln.android.data.model.UserSearchResponse
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.util.formatPresenceLabel
import nhom3.chatbln.android.ui.viewmodel.MainChatUiState
import nhom3.chatbln.android.ui.viewmodel.UserRelationshipState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    uiState: MainChatUiState,
    resolveAssetUrl: (String?) -> String?,
    onContactsSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onFriendClick: (FriendResponse) -> Unit,
    onUnfriend: (FriendResponse) -> Unit,
    onUserSearchKeywordChange: (String) -> Unit,
    onExecuteUserSearch: () -> Unit,
    onResetUserSearch: () -> Unit,
    resolveRelationship: (UserSearchResponse) -> UserRelationshipState,
    onSendFriendRequest: (UserSearchResponse) -> Unit
) {
    var showAddFriendSheet by rememberSaveable { mutableStateOf(false) }
    var friendPendingUnfriend by remember { mutableStateOf<FriendResponse?>(null) }

    val filteredFriends = remember(uiState.friends, uiState.contactsSearchQuery) {
        val query = uiState.contactsSearchQuery.trim()
        val base = if (query.isEmpty()) {
            uiState.friends
        } else {
            uiState.friends.filter { friend ->
                (friend.fullName.orEmpty()).contains(query, ignoreCase = true) ||
                    friend.username.contains(query, ignoreCase = true)
            }
        }
        base.sortedWith(
            compareByDescending<FriendResponse> { it.isOnline }
                .thenBy { (it.fullName ?: it.username).lowercase() }
        )
    }

    friendPendingUnfriend?.let { target ->
        val targetName = target.fullName?.takeIf { it.isNotBlank() } ?: target.username
        AlertDialog(
            onDismissRequest = { friendPendingUnfriend = null },
            title = { Text("Hủy kết bạn") },
            text = {
                Text(
                    "Hủy kết bạn với \"$targetName\"? Hai bạn sẽ trở về trạng thái chưa kết bạn và có thể kết bạn lại bất cứ lúc nào."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        friendPendingUnfriend = null
                        onUnfriend(target)
                    }
                ) {
                    Text("Hủy kết bạn", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { friendPendingUnfriend = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (showAddFriendSheet) {
        AddFriendBottomSheet(
            keyword = uiState.userSearchKeyword,
            hasSearched = uiState.hasSearchedUsers,
            isSearching = uiState.isSearchingUsers,
            results = uiState.userSearchResults,
            sendingUserIds = uiState.sendingFriendRequestUserIds,
            feedbackMessage = uiState.addFriendFeedback,
            isFeedbackError = uiState.addFriendFeedbackIsError,
            resolveAssetUrl = resolveAssetUrl,
            resolveRelationship = resolveRelationship,
            onKeywordChange = onUserSearchKeywordChange,
            onSearchSubmit = onExecuteUserSearch,
            onSendRequest = onSendFriendRequest,
            onDismiss = {
                showAddFriendSheet = false
                onResetUserSearch()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Danh bạ (${uiState.friends.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Làm mới danh bạ"
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
                onClick = { showAddFriendSheet = true },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null
                    )
                },
                text = { Text("Thêm bạn") }
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
                value = uiState.contactsSearchQuery,
                onValueChange = onContactsSearchChange,
                placeholder = { Text("Tìm trong danh sách bạn bè...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.contactsSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onContactsSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Xóa từ khóa"
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
                uiState.isFriendsLoading && uiState.friends.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                filteredFriends.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.contactsSearchQuery.isBlank()) {
                                "Chưa có bạn bè trong danh bạ"
                            } else {
                                "Không tìm thấy người bạn nào khớp từ khóa"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nhấn nút Thêm bạn để tìm kiếm theo tên hoặc @username.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.contactsSearchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(onClick = { showAddFriendSheet = true }) {
                                Text("Tìm bạn bè mới")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(items = filteredFriends, key = { it.id }) { friend ->
                            val displayName = friend.fullName?.takeIf { it.isNotBlank() }
                                ?: friend.username
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFriendClick(friend) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarView(
                                    displayName = displayName,
                                    avatarUrl = resolveAssetUrl(friend.avatarUrl),
                                    size = 48.dp,
                                    isOnline = friend.isOnline,
                                    showOnlineBadge = true
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${friend.username} · ${
                                            formatPresenceLabel(friend.isOnline, friend.lastSeen)
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (friend.isOnline) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onFriendClick(friend) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Nhắn tin",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { friendPendingUnfriend = friend }) {
                                    Icon(
                                        imageVector = Icons.Outlined.PersonRemove,
                                        contentDescription = "Hủy kết bạn",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
