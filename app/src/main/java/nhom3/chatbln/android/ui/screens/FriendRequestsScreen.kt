package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Outbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.model.FriendshipResponse
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.components.TonalListSkeleton
import nhom3.chatbln.android.ui.util.formatMessageTime
import nhom3.chatbln.android.ui.viewmodel.MainChatUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    uiState: MainChatUiState,
    resolveAssetUrl: (String?) -> String?,
    onRefresh: () -> Unit,
    onAccept: (FriendshipResponse) -> Unit,
    onReject: (FriendshipResponse) -> Unit,
    onCancelSent: (FriendshipResponse) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lời mời kết bạn",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Làm mới lời mời kết bạn"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Đã nhận (${uiState.receivedRequests.size})") },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Đã gửi (${uiState.sentRequests.size})") },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }

            when {
                uiState.isRequestsLoading &&
                    uiState.receivedRequests.isEmpty() &&
                    uiState.sentRequests.isEmpty() -> {
                    TonalListSkeleton(rowCount = 5, showTrailingAction = true)
                }

                selectedTab == 0 -> {
                    if (uiState.receivedRequests.isEmpty()) {
                        EmptyRequestsState(
                            icon = Icons.Outlined.HowToReg,
                            title = "Không có lời mời kết bạn mới",
                            subtitle = "Khi người khác gửi lời mời kết bạn, bạn có thể xác nhận hoặc từ chối tại đây."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.receivedRequests,
                                key = { "received_${it.id}" },
                                contentType = { "received_request" }
                            ) { req ->
                                val isProcessing = uiState.processingRequestIds.contains(req.id)
                                val displayName = req.requesterFullName
                                    ?.takeIf { it.isNotBlank() }
                                    ?: req.requesterUsername
                                    ?: "Người dùng #${req.requesterId}"
                                val usernameSub = req.requesterUsername
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { "@$it" }
                                    .orEmpty()
                                val timeSub = formatMessageTime(req.createdAt)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarView(
                                            displayName = displayName,
                                            avatarUrl = resolveAssetUrl(req.requesterAvatarUrl),
                                            size = 48.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val meta = listOf(usernameSub, timeSub)
                                                .filter { it.isNotEmpty() }
                                                .joinToString(" · ")
                                            if (meta.isNotEmpty()) {
                                                Text(
                                                    text = meta,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onReject(req) },
                                            enabled = !isProcessing,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Từ chối")
                                        }
                                        Button(
                                            onClick = { onAccept(req) },
                                            enabled = !isProcessing,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            if (isProcessing) {
                                                CircularProgressIndicator(
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Text("Chấp nhận")
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }

                else -> {
                    if (uiState.sentRequests.isEmpty()) {
                        EmptyRequestsState(
                            icon = Icons.Outlined.Outbox,
                            title = "Chưa có lời mời đã gửi",
                            subtitle = "Các lời mời kết bạn đang chờ đối phương phản hồi sẽ hiển thị ở đây."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = uiState.sentRequests,
                                key = { "sent_${it.id}" },
                                contentType = { "sent_request" }
                            ) { req ->
                                val isProcessing = uiState.processingRequestIds.contains(req.id)
                                val knownTarget = uiState.knownUsersById[req.addresseeId]
                                val targetName = req.addresseeFullName?.takeIf { it.isNotBlank() }
                                    ?: knownTarget?.fullName?.takeIf { it.isNotBlank() }
                                    ?: req.addresseeUsername?.takeIf { it.isNotBlank() }
                                    ?: knownTarget?.username
                                    ?: "Người nhận #${req.addresseeId}"
                                val targetUsername = (req.addresseeUsername?.takeIf { it.isNotBlank() }
                                    ?: knownTarget?.username)
                                    ?.let { "@$it" }
                                val targetAvatar = req.addresseeAvatarUrl ?: knownTarget?.avatarUrl
                                val timeText = formatMessageTime(req.createdAt)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarView(
                                        displayName = targetName,
                                        avatarUrl = resolveAssetUrl(targetAvatar),
                                        size = 46.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targetName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sub = listOfNotNull(
                                            targetUsername,
                                            timeText.takeIf { it.isNotBlank() }
                                        ).joinToString(" · ")
                                        if (sub.isNotEmpty()) {
                                            Text(
                                                text = sub,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { onCancelSent(req) },
                                        enabled = !isProcessing,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        if (isProcessing) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else {
                                            Text("Thu hồi")
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRequestsState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
