package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import nhom3.chatbln.android.data.model.ConversationItemResponse
import nhom3.chatbln.android.data.model.MessageDto
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.util.formatMessageTime
import nhom3.chatbln.android.ui.util.formatPresenceLabel
import nhom3.chatbln.android.ui.util.formatVietnameseDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationInfoSheet(
    conversation: ConversationItemResponse,
    messages: List<MessageDto>,
    resolveAssetUrl: (String?) -> String?,
    onPreviewImage: (String) -> Unit,
    onDeleteConversation: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    val title = conversation.name?.takeIf { it.isNotBlank() } ?: "Hội thoại #${conversation.id}"

    if (showDeleteConfirm && onDeleteConversation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa lịch sử trò chuyện") },
            text = { Text("Xóa cuộc trò chuyện và toàn bộ tin nhắn với \"$title\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteConversation()
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    val sharedImages = remember(messages) {
        messages.asReversed().mapNotNull { msg ->
            if (msg.attachmentType.equals("IMAGE", ignoreCase = true)) {
                resolveAssetUrl(msg.attachmentUrl)
            } else {
                null
            }
        }
    }

        val sharedFilesAndVideos = remember(messages) {
        messages.asReversed().filter { msg ->
            val type = msg.attachmentType?.uppercase().orEmpty()
            (type == "FILE" || type == "VIDEO") && !msg.attachmentUrl.isNullOrBlank()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AvatarView(
                    displayName = title,
                    avatarUrl = resolveAssetUrl(conversation.avatarUrl),
                    size = 72.dp,
                    isOnline = conversation.isOnline == true,
                    showOnlineBadge = !conversation.isGroup,
                    isGroup = conversation.isGroup
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (conversation.isGroup) {
                        "Nhóm trò chuyện"
                    } else {
                        val usernamePart = conversation.targetUsername
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "@$it · " }
                            .orEmpty()
                        usernamePart + formatPresenceLabel(conversation.isOnline, conversation.lastSeen)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Thông tin") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Ảnh (${sharedImages.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tệp & Video (${sharedFilesAndVideos.size})") }
                )
            }

            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (conversation.isGroup) {
                            InfoRow(label = "Loại hội thoại", value = "Nhóm trò chuyện chung")
                            InfoRow(label = "Số tin nhắn đã tải", value = "${messages.size} tin nhắn")
                        } else {
                            InfoRow(
                                label = "Tên người dùng",
                                value = conversation.targetUsername?.let { "@$it" } ?: "Chưa cập nhật"
                            )
                            InfoRow(
                                label = "Email",
                                value = conversation.email?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật"
                            )
                            InfoRow(
                                label = "Số điện thoại",
                                value = conversation.phone?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật"
                            )
                            InfoRow(
                                label = "Giới tính",
                                value = conversation.gender?.takeIf { it.isNotBlank() } ?: "Chưa cập nhật"
                            )
                            InfoRow(
                                label = "Ngày sinh",
                                value = formatVietnameseDate(conversation.dob)
                            )
                            InfoRow(
                                label = "Tiểu sử",
                                value = conversation.bio?.takeIf { it.isNotBlank() } ?: "Chưa có tiểu sử."
                            )
                        }

                        if (onDeleteConversation != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Xóa lịch sử trò chuyện")
                            }
                        }
                    }
                }

                1 -> {
                    if (sharedImages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có hình ảnh nào được chia sẻ.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                        ) {
                            items(sharedImages) { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Ảnh chia sẻ",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { onPreviewImage(imageUrl) }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    if (sharedFilesAndVideos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có tệp hoặc video nào được chia sẻ.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            items(items = sharedFilesAndVideos, key = { it.id }) { msg ->
                                val isVideo = msg.attachmentType.equals("VIDEO", ignoreCase = true)
                                val url = resolveAssetUrl(msg.attachmentUrl)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !url.isNullOrBlank()) {
                                            url?.let { runCatching { uriHandler.openUri(it) } }
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isVideo) {
                                            Icons.Outlined.PlayCircleOutline
                                        } else {
                                            Icons.Outlined.AttachFile
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = msg.attachmentName?.takeIf { it.isNotBlank() }
                                                ?: if (isVideo) "Video đính kèm" else "Tệp đính kèm",
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val meta = listOfNotNull(
                                            msg.attachmentSize?.takeIf { it.isNotBlank() },
                                            formatMessageTime(msg.createdAt).takeIf { it.isNotBlank() }
                                        ).joinToString(" · ")
                                        if (meta.isNotEmpty()) {
                                            Text(
                                                text = meta,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                        contentDescription = "Mở",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
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
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}
