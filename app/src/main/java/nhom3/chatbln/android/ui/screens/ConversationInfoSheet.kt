package nhom3.chatbln.android.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import nhom3.chatbln.android.data.model.ConversationItemResponse
import nhom3.chatbln.android.data.model.GroupJoinRequestDto
import nhom3.chatbln.android.data.model.GroupMemberDto
import nhom3.chatbln.android.data.model.MessageDto
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.components.TonalListSkeleton
import nhom3.chatbln.android.ui.components.resolvePeerPresenceIcon
import nhom3.chatbln.android.ui.util.buildGroupShareLink
import nhom3.chatbln.android.ui.util.formatMessageTime
import nhom3.chatbln.android.ui.util.formatPresenceLabel
import nhom3.chatbln.android.ui.util.formatVietnameseDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationInfoSheet(
    conversation: ConversationItemResponse,
    messages: List<MessageDto>,
    currentUsername: String? = null,
    groupMembers: List<GroupMemberDto> = emptyList(),
    joinRequests: List<GroupJoinRequestDto> = emptyList(),
    isUpdatingGroupSettings: Boolean = false,
    processingMemberIds: Set<Long> = emptySet(),
    processingJoinRequestIds: Set<Long> = emptySet(),
    friendIds: Set<Long> = emptySet(),
    resolveAssetUrl: (String?) -> String?,
    onPreviewImage: (String) -> Unit,
    onUnfriend: (() -> Unit)? = null,
    onReFriend: (() -> Unit)? = null,
    onMessageMember: ((GroupMemberDto) -> Unit)? = null,
    onAddFriendMember: ((GroupMemberDto) -> Unit)? = null,
    onUpdateGroupVisibility: ((String) -> Unit)? = null,
    onRegenerateInviteCode: (() -> Unit)? = null,
    onApproveJoinRequest: ((GroupJoinRequestDto) -> Unit)? = null,
    onRejectJoinRequest: ((GroupJoinRequestDto) -> Unit)? = null,
    onUpdateMemberRole: ((GroupMemberDto, String) -> Unit)? = null,
    onTransferLeadership: ((GroupMemberDto) -> Unit)? = null,
    onDeleteConversation: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDeleteOrLeaveConfirm by rememberSaveable { mutableStateOf(false) }
    var showUnfriendConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingLeaderCandidate by remember { mutableStateOf<GroupMemberDto?>(null) }
    var copiedInviteFeedback by remember { mutableStateOf(false) }

    val title = conversation.name?.takeIf { it.isNotBlank() } ?: "Hội thoại #${conversation.id}"

    // Determine effective role from members list if available, falling back to conversation.currentUserRole
    val effectiveCurrentRole = remember(groupMembers, currentUsername, conversation.currentUserRole) {
        val fromMemberList = groupMembers.firstOrNull {
            !currentUsername.isNullOrBlank() && it.username.equals(currentUsername, ignoreCase = true)
        }?.role
        fromMemberList ?: conversation.currentUserRole
    }
    val isGroupAdmin = effectiveCurrentRole.equals("ADMIN", ignoreCase = true)
    val isGroupDeputy = effectiveCurrentRole.equals("DEPUTY", ignoreCase = true)
    val canManageInvitesAndApprovals = isGroupAdmin || isGroupDeputy

    val effectivePendingCount = if (joinRequests.isNotEmpty()) {
        joinRequests.size
    } else {
        conversation.pendingJoinRequestCount ?: 0
    }

    if (showDeleteOrLeaveConfirm && onDeleteConversation != null) {
        val dialogTitle = when {
            !conversation.isGroup -> "Xóa lịch sử trò chuyện"
            isGroupAdmin -> "Giải tán nhóm trò chuyện"
            else -> "Rời khỏi nhóm trò chuyện"
        }
        val dialogBody = when {
            !conversation.isGroup ->
                "Xóa cuộc trò chuyện và toàn bộ tin nhắn với \"$title\"?"
            isGroupAdmin ->
                "Giải tán nhóm \"$title\"? Nhóm và toàn bộ lịch sử tin nhắn sẽ bị xóa vĩnh viễn đối với tất cả thành viên."
            else ->
                "Bạn muốn rời khỏi nhóm \"$title\"? Bạn có thể tham gia lại sau qua liên kết mời."
        }
        val confirmLabel = when {
            !conversation.isGroup -> "Xóa"
            isGroupAdmin -> "Giải tán nhóm"
            else -> "Rời khỏi nhóm"
        }

        AlertDialog(
            onDismissRequest = { showDeleteOrLeaveConfirm = false },
            title = {
                Text(
                    text = dialogTitle,
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = { Text(dialogBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteOrLeaveConfirm = false
                        onDeleteConversation()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(confirmLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteOrLeaveConfirm = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    pendingLeaderCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { pendingLeaderCandidate = null },
            title = {
                Text(
                    text = "Chuyển quyền Trưởng nhóm",
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = {
                Text(
                    "Chuyển quyền Trưởng nhóm của \"$title\" cho ${candidate.displayName} (@${candidate.username})? Bạn sẽ trở thành Thành viên của nhóm."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = candidate
                        pendingLeaderCandidate = null
                        onTransferLeadership?.invoke(target)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Xác nhận chuyển quyền")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingLeaderCandidate = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showUnfriendConfirm && onUnfriend != null) {
        AlertDialog(
            onDismissRequest = { showUnfriendConfirm = false },
            title = {
                Text(
                    text = "Hủy kết bạn",
                    modifier = Modifier.semantics { heading() }
                )
            },
            text = {
                Text(
                    "Hủy kết bạn với \"$title\"? Hai bạn sẽ trở về trạng thái chưa kết bạn và có thể kết bạn lại bất cứ lúc nào."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnfriendConfirm = false
                        onUnfriend()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Hủy kết bạn", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnfriendConfirm = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Đóng")
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

    val memberTotal = if (groupMembers.isNotEmpty()) {
        groupMembers.size
    } else {
        conversation.memberCount ?: 0
    }
    val (peerIcon, peerColor) = resolvePeerPresenceIcon(
        isOnline = conversation.isOnline,
        presenceStatus = conversation.presenceStatus
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
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
                    presenceStatus = conversation.presenceStatus,
                    showOnlineBadge = !conversation.isGroup,
                    isGroup = conversation.isGroup
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )

                if (conversation.isGroup) {
                    Text(
                        text = if (memberTotal > 0) "$memberTotal thành viên" else "Nhóm trò chuyện",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = peerIcon,
                            contentDescription = null,
                            tint = peerColor,
                            modifier = Modifier.size(13.dp)
                        )
                        val usernamePart = conversation.targetUsername
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "@$it · " }
                            .orEmpty()
                        Text(
                            text = usernamePart + formatPresenceLabel(
                                conversation.isOnline,
                                conversation.lastSeen,
                                conversation.presenceStatus
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (conversation.isGroup) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (conversation.isPrivateGroup) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (conversation.isPrivateGroup) {
                                        Icons.Outlined.Lock
                                    } else {
                                        Icons.Outlined.Public
                                    },
                                    contentDescription = null,
                                    tint = if (conversation.isPrivateGroup) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = conversation.groupVisibilityLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (conversation.isPrivateGroup) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }

                        GroupRoleBadge(role = effectiveCurrentRole)

                        if (canManageInvitesAndApprovals && effectivePendingCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "$effectivePendingCount yêu cầu chờ duyệt",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.heightIn(min = 48.dp),
                    text = {
                        Text(
                            if (conversation.isGroup) {
                                "Quản lý ($memberTotal)"
                            } else {
                                "Thông tin"
                            }
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.heightIn(min = 48.dp),
                    text = { Text("Ảnh (${sharedImages.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.heightIn(min = 48.dp),
                    text = { Text("Tệp & Video (${sharedFilesAndVideos.size})") }
                )
            }

            when (selectedTab) {
                0 -> {
                    if (conversation.isGroup) {
                        GroupManagementTabContent(
                            conversation = conversation,
                            groupTitle = title,
                            currentUsername = currentUsername,
                            isGroupAdmin = isGroupAdmin,
                            canManageInvitesAndApprovals = canManageInvitesAndApprovals,
                            groupMembers = groupMembers,
                            joinRequests = joinRequests,
                            isUpdatingGroupSettings = isUpdatingGroupSettings,
                            processingMemberIds = processingMemberIds,
                            processingJoinRequestIds = processingJoinRequestIds,
                            friendIds = friendIds,
                            copiedInviteFeedback = copiedInviteFeedback,
                            resolveAssetUrl = resolveAssetUrl,
                            onCopyInviteLink = { link ->
                                clipboardManager.setText(AnnotatedString(link))
                                copiedInviteFeedback = true
                            },
                            onShareInviteLink = { link ->
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Tham gia nhóm \"$title\" trên Chat BLN: $link"
                                    )
                                }
                                runCatching {
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, "Chia sẻ liên kết nhóm")
                                    )
                                }
                            },
                            onUpdateGroupVisibility = onUpdateGroupVisibility,
                            onRegenerateInviteCode = {
                                copiedInviteFeedback = false
                                onRegenerateInviteCode?.invoke()
                            },
                            onApproveJoinRequest = onApproveJoinRequest,
                            onRejectJoinRequest = onRejectJoinRequest,
                            onMessageMember = onMessageMember,
                            onAddFriendMember = onAddFriendMember,
                            onUpdateMemberRole = onUpdateMemberRole,
                            onRequestTransferLeadership = { candidate ->
                                pendingLeaderCandidate = candidate
                            },
                            onDangerAction = if (onDeleteConversation != null) {
                                { showDeleteOrLeaveConfirm = true }
                            } else {
                                null
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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

                            Spacer(modifier = Modifier.height(4.dp))

                            if (conversation.isFriend != false && onUnfriend != null) {
                                OutlinedButton(
                                    onClick = { showUnfriendConfirm = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PersonRemove,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Hủy kết bạn")
                                }
                            } else if (conversation.isFriend == false) {
                                if (conversation.friendshipStatus.equals("PENDING", ignoreCase = true)) {
                                    OutlinedButton(
                                        onClick = {},
                                        enabled = false,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Text("Đã gửi lời mời kết bạn")
                                    }
                                } else if (onReFriend != null) {
                                    Button(
                                        onClick = onReFriend,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Kết bạn lại")
                                    }
                                }
                            }

                            if (onDeleteConversation != null) {
                                OutlinedButton(
                                    onClick = { showDeleteOrLeaveConfirm = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
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
                                .heightIn(max = 340.dp)
                        ) {
                            itemsIndexed(
                                items = sharedImages,
                                key = { index, imageUrl -> "${index}_$imageUrl" },
                                contentType = { _, _ -> "shared_image" }
                            ) { _, imageUrl ->
                                val imageRequest = remember(imageUrl, context) {
                                    ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .memoryCacheKey(imageUrl)
                                        .diskCacheKey(imageUrl)
                                        .crossfade(false)
                                        .build()
                                }
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Mở ảnh chia sẻ",
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
                                .heightIn(max = 340.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            items(
                                items = sharedFilesAndVideos,
                                key = { it.id },
                                contentType = { it.attachmentType ?: "FILE" }
                            ) { msg ->
                                val isVideo = msg.attachmentType.equals("VIDEO", ignoreCase = true)
                                val url = resolveAssetUrl(msg.attachmentUrl)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
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
                                        contentDescription = "Mở tệp",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupManagementTabContent(
    conversation: ConversationItemResponse,
    groupTitle: String,
    currentUsername: String?,
    isGroupAdmin: Boolean,
    canManageInvitesAndApprovals: Boolean,
    groupMembers: List<GroupMemberDto>,
    joinRequests: List<GroupJoinRequestDto>,
    isUpdatingGroupSettings: Boolean,
    processingMemberIds: Set<Long>,
    processingJoinRequestIds: Set<Long>,
    friendIds: Set<Long>,
    copiedInviteFeedback: Boolean,
    resolveAssetUrl: (String?) -> String?,
    onCopyInviteLink: (String) -> Unit,
    onShareInviteLink: (String) -> Unit,
    onUpdateGroupVisibility: ((String) -> Unit)?,
    onRegenerateInviteCode: () -> Unit,
    onApproveJoinRequest: ((GroupJoinRequestDto) -> Unit)?,
    onRejectJoinRequest: ((GroupJoinRequestDto) -> Unit)?,
    onMessageMember: ((GroupMemberDto) -> Unit)?,
    onAddFriendMember: ((GroupMemberDto) -> Unit)?,
    onUpdateMemberRole: ((GroupMemberDto, String) -> Unit)?,
    onRequestTransferLeadership: (GroupMemberDto) -> Unit,
    onDangerAction: (() -> Unit)?
) {
    val inviteCode = conversation.inviteCode?.takeIf { it.isNotBlank() }
    val inviteLink = inviteCode?.let(::buildGroupShareLink)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Share Invite Link + Visibility Section
        item(
            key = "invite_and_visibility_section",
            contentType = "invite_section"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Liên kết mời tham gia nhóm",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )

                if (inviteLink != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = inviteLink,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onCopyInviteLink(inviteLink) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (copiedInviteFeedback) {
                                            Icons.Outlined.Check
                                        } else {
                                            Icons.Outlined.ContentCopy
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (copiedInviteFeedback) "Đã sao chép" else "Sao chép")
                                }

                                OutlinedButton(
                                    onClick = { onShareInviteLink(inviteLink) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chia sẻ")
                                }

                                if (canManageInvitesAndApprovals) {
                                    OutlinedButton(
                                        onClick = onRegenerateInviteCode,
                                        enabled = !isUpdatingGroupSettings,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Đổi mã mời")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Nhóm chưa có mã mời hoạt động.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (canManageInvitesAndApprovals) {
                        OutlinedButton(
                            onClick = onRegenerateInviteCode,
                            enabled = !isUpdatingGroupSettings,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Tạo liên kết mời cho \"$groupTitle\"")
                        }
                    }
                }

                if (isGroupAdmin && onUpdateGroupVisibility != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Chế độ nhóm (Trưởng nhóm)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = !conversation.isPrivateGroup,
                                onClick = {
                                    if (conversation.isPrivateGroup) {
                                        onUpdateGroupVisibility("PUBLIC")
                                    }
                                },
                                enabled = !isUpdatingGroupSettings,
                                label = { Text("Nhóm công khai") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                            FilterChip(
                                selected = conversation.isPrivateGroup,
                                onClick = {
                                    if (!conversation.isPrivateGroup) {
                                        onUpdateGroupVisibility("PRIVATE")
                                    }
                                },
                                enabled = !isUpdatingGroupSettings,
                                label = { Text("Nhóm riêng tư") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                )
            }
        }

        // 2. Pending Join Requests Section (for ADMIN & DEPUTY)
        if (canManageInvitesAndApprovals && (conversation.isPrivateGroup || joinRequests.isNotEmpty())) {
            item(
                key = "pending_join_requests_header",
                contentType = "section_header"
            ) {
                Text(
                    text = "Yêu cầu tham gia chờ duyệt (${joinRequests.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }

            if (joinRequests.isEmpty()) {
                item(
                    key = "pending_join_requests_empty",
                    contentType = "empty_notice"
                ) {
                    Text(
                        text = "Không có yêu cầu tham gia nào đang chờ phê duyệt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = joinRequests,
                    key = { "join_req_${it.id}" },
                    contentType = { "join_request" }
                ) { req ->
                    val isProcessing = processingJoinRequestIds.contains(req.id)
                    val timeLabel = formatMessageTime(req.createdAt)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarView(
                                displayName = req.displayName,
                                avatarUrl = resolveAssetUrl(req.avatarUrl),
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sub = listOfNotNull(
                                    "@${req.username}",
                                    timeLabel.takeIf { it.isNotBlank() }
                                ).joinToString(" · ")
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            OutlinedButton(
                                onClick = { onRejectJoinRequest?.invoke(req) },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Từ chối")
                            }
                            Button(
                                onClick = { onApproveJoinRequest?.invoke(req) },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text("Duyệt")
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }

        // 3. Members List Section
        item(
            key = "members_header",
            contentType = "section_header"
        ) {
            Text(
                text = "Danh sách thành viên (${groupMembers.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        }

        if (groupMembers.isEmpty()) {
            item(
                key = "members_loading",
                contentType = "skeleton"
            ) {
                TonalListSkeleton(rowCount = 3)
            }
        } else {
            items(
                items = groupMembers,
                key = { "member_${it.id}" },
                contentType = { "group_member" }
            ) { member ->
                val isMe = member.username.equals(currentUsername, ignoreCase = true)
                val isFriend = member.id in friendIds
                val isBusy = processingMemberIds.contains(member.id)
                var showRoleMenu by remember { mutableStateOf(false) }
                val (memberIcon, memberColor) = resolvePeerPresenceIcon(
                    isOnline = member.isOnline,
                    presenceStatus = member.presenceStatus
                )

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarView(
                            displayName = member.displayName,
                            avatarUrl = resolveAssetUrl(member.avatarUrl),
                            size = 42.dp,
                            isOnline = member.isOnline,
                            presenceStatus = member.presenceStatus,
                            showOnlineBadge = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = member.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isMe) {
                                    Text(
                                        text = "(Bạn)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = memberIcon,
                                    contentDescription = null,
                                    tint = memberColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "@${member.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                GroupRoleBadge(role = member.role)
                            }
                        }

                        if (!isMe) {
                            if (isFriend && onMessageMember != null) {
                                IconButton(
                                    onClick = { onMessageMember(member) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Nhắn tin riêng với ${member.displayName}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (!isFriend && onAddFriendMember != null) {
                                IconButton(
                                    onClick = { onAddFriendMember(member) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.PersonAdd,
                                        contentDescription = "Kết bạn với ${member.displayName}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (isGroupAdmin) {
                                Box {
                                    IconButton(
                                        onClick = { showRoleMenu = true },
                                        enabled = !isBusy,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        if (isBusy) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.MoreVert,
                                                contentDescription = "Quản lý vai trò của ${member.displayName}"
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showRoleMenu,
                                        onDismissRequest = { showRoleMenu = false }
                                    ) {
                                        if (onUpdateMemberRole != null) {
                                            val targetRole = if (member.isDeputy) "MEMBER" else "DEPUTY"
                                            val roleActionText = if (member.isDeputy) {
                                                "Gỡ Phó nhóm"
                                            } else {
                                                "Bổ nhiệm Phó nhóm"
                                            }
                                            DropdownMenuItem(
                                                text = { Text(roleActionText) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AdminPanelSettings,
                                                        contentDescription = null
                                                    )
                                                },
                                                onClick = {
                                                    showRoleMenu = false
                                                    onUpdateMemberRole(member, targetRole)
                                                }
                                            )
                                        }

                                        DropdownMenuItem(
                                            text = { Text("Chuyển quyền Trưởng nhóm") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.SwapHoriz,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showRoleMenu = false
                                                onRequestTransferLeadership(member)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }

        // 4. Dissolve / Leave Group Danger Button
        if (onDangerAction != null) {
            item(
                key = "group_danger_button",
                contentType = "danger_action"
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onDangerAction,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = if (isGroupAdmin) {
                            Icons.Outlined.DeleteOutline
                        } else {
                            Icons.AutoMirrored.Outlined.ExitToApp
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGroupAdmin) "Giải tán nhóm" else "Rời khỏi nhóm")
                }
            }
        }
    }
}

@Composable
private fun GroupRoleBadge(role: String?) {
    val upper = role?.trim()?.uppercase().orEmpty()
    val (label, bgColor, fgColor) = when (upper) {
        "ADMIN" -> Triple(
            "Trưởng nhóm",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "DEPUTY" -> Triple(
            "Phó nhóm",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        else -> Triple(
            "Thành viên",
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fgColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
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
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}
