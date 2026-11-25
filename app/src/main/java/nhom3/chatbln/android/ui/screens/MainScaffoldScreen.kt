package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nhom3.chatbln.android.ui.viewmodel.MainChatViewModel

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CONVERSATIONS(
        label = "Đoạn chat",
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    ),
    CONTACTS(
        label = "Danh bạ",
        selectedIcon = Icons.Filled.Contacts,
        unselectedIcon = Icons.Outlined.Contacts
    ),
    REQUESTS(
        label = "Lời mời",
        selectedIcon = Icons.Filled.PersonAdd,
        unselectedIcon = Icons.Outlined.PersonAdd
    ),
    PROFILE(
        label = "Hồ sơ",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline
    )
}

@Composable
fun MainScaffoldScreen(
    viewModel: MainChatViewModel,
    isDarkThemeEffective: Boolean,
    onOpenConversation: (Long) -> Unit,
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.CONVERSATIONS) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val badgeCount = when (tab) {
                        MainTab.CONVERSATIONS -> uiState.totalUnreadCount
                        MainTab.REQUESTS -> uiState.receivedRequestsBadgeCount
                        else -> 0
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge {
                                            Text(
                                                text = if (badgeCount > 99) "99+" else badgeCount.toString()
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.CONVERSATIONS -> {
                    ConversationsScreen(
                        uiState = uiState,
                        resolveAssetUrl = viewModel::resolveAssetUrl,
                        onSearchQueryChange = viewModel::setConversationSearchQuery,
                        onRefresh = viewModel::refreshConversations,
                        onChangePresenceMode = viewModel::setPresenceMode,
                        onConversationClick = onOpenConversation,
                        onStartPrivateChat = { friend ->
                            viewModel.openOrCreatePrivateChat(
                                targetUserId = friend.id,
                                fallbackName = friend.fullName ?: friend.username,
                                fallbackUsername = friend.username,
                                fallbackAvatarUrl = friend.avatarUrl,
                                fallbackOnline = friend.isOnline,
                                onReady = onOpenConversation
                            )
                        },
                        onCreateGroupChat = { groupName, memberIds ->
                            viewModel.createGroupChat(
                                name = groupName,
                                memberIds = memberIds,
                                onReady = onOpenConversation
                            )
                        }
                    )
                }

                MainTab.CONTACTS -> {
                    ContactsScreen(
                        uiState = uiState,
                        resolveAssetUrl = viewModel::resolveAssetUrl,
                        onContactsSearchChange = viewModel::setContactsSearchQuery,
                        onRefresh = viewModel::refreshFriendsAndRequests,
                        onFriendClick = { friend ->
                            viewModel.openOrCreatePrivateChat(
                                targetUserId = friend.id,
                                fallbackName = friend.fullName ?: friend.username,
                                fallbackUsername = friend.username,
                                fallbackAvatarUrl = friend.avatarUrl,
                                fallbackOnline = friend.isOnline,
                                onReady = onOpenConversation
                            )
                        },
                        onUnfriend = { friend ->
                            viewModel.unfriendUser(
                                targetUserId = friend.id,
                                displayName = friend.fullName?.takeIf { it.isNotBlank() } ?: friend.username
                            )
                        },
                        onUserSearchKeywordChange = viewModel::onUserSearchKeywordChange,
                        onExecuteUserSearch = { viewModel.executeUserSearch() },
                        onResetUserSearch = viewModel::resetUserSearch,
                        resolveRelationship = { user ->
                            viewModel.resolveUserRelationship(user, uiState)
                        },
                        onSendFriendRequest = viewModel::sendFriendRequest
                    )
                }

                MainTab.REQUESTS -> {
                    FriendRequestsScreen(
                        uiState = uiState,
                        resolveAssetUrl = viewModel::resolveAssetUrl,
                        onRefresh = viewModel::refreshFriendsAndRequests,
                        onAccept = viewModel::acceptFriendRequest,
                        onReject = viewModel::rejectFriendRequest,
                        onCancelSent = viewModel::cancelSentFriendRequest
                    )
                }

                MainTab.PROFILE -> {
                    ProfileScreen(
                        uiState = uiState,
                        isDarkThemeEffective = isDarkThemeEffective,
                        resolveAssetUrl = viewModel::resolveAssetUrl,
                        onUploadAvatar = viewModel::uploadAvatarAndSave,
                        onSaveProfile = { fullName, email, phone, gender, dob, bio, avatarUrl ->
                            viewModel.updateProfile(
                                fullName = fullName,
                                email = email,
                                phone = phone,
                                gender = gender,
                                dob = dob,
                                bio = bio,
                                avatarUrl = avatarUrl
                            )
                        },
                        onToggleDarkMode = viewModel::setDarkMode,
                        onChangePresenceMode = viewModel::setPresenceMode,
                        onUpdateServerUrl = viewModel::updateServerUrl,
                        onLogout = { viewModel.logout(onLoggedOut) }
                    )
                }
            }
        }
    }
}
