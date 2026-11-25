package nhom3.chatbln.android.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import nhom3.chatbln.android.data.model.ConversationItemResponse
import nhom3.chatbln.android.data.model.MessageDto
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.util.MarkdownLinkText
import nhom3.chatbln.android.ui.util.extractFirstLink
import nhom3.chatbln.android.ui.util.extractHostName
import nhom3.chatbln.android.ui.util.formatMessageTime
import nhom3.chatbln.android.ui.util.formatPresenceLabel
import nhom3.chatbln.android.ui.viewmodel.MainChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    conversationId: Long,
    viewModel: MainChatViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var composerValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showEmojiSheet by rememberSaveable { mutableStateOf(false) }
    var showInfoSheet by rememberSaveable { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var previewImageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf(setOf<Long>()) }
    var pendingDeleteIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(conversationId) {
        isSelectionMode = false
        selectedMessageIds = emptySet()
        pendingDeleteIds = emptyList()
        viewModel.openConversation(conversationId)
    }

    DisposableEffect(conversationId) {
        onDispose {
            viewModel.closeActiveConversation()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val conversation = remember(uiState.conversations, conversationId) {
        uiState.conversations.firstOrNull { it.id == conversationId }
            ?: ConversationItemResponse(id = conversationId, name = "Hội thoại #$conversationId")
    }

    val allMessages = uiState.messagesByConversation[conversationId].orEmpty()
    val groupMembers = uiState.groupMembersByConversation[conversationId]
        ?: conversation.members.orEmpty()
    val friendIdSet = remember(uiState.friends) { uiState.friends.map { it.id }.toSet() }
    val hasMore = uiState.hasMoreByConversation[conversationId] == true

    LaunchedEffect(allMessages) {
        if (selectedMessageIds.isNotEmpty()) {
            val existingIds = allMessages.map { it.id }.toHashSet()
            val pruned = selectedMessageIds.intersect(existingIds)
            if (pruned.size != selectedMessageIds.size) {
                selectedMessageIds = pruned
            }
        }
    }

    val visibleMessages = remember(allMessages, showSearchBar, uiState.inChatSearchQuery) {
        val q = uiState.inChatSearchQuery.trim()
        if (!showSearchBar || q.isEmpty()) {
            allMessages
        } else {
            allMessages.filter { m ->
                m.content.contains(q, ignoreCase = true) ||
                    (m.attachmentName.orEmpty()).contains(q, ignoreCase = true)
            }
        }
    }

    // Auto-scroll to latest message when new message arrives at the end
    val latestMessageId = visibleMessages.lastOrNull()?.id
    LaunchedEffect(latestMessageId) {
        if (visibleMessages.isNotEmpty() && !showSearchBar) {
            listState.animateScrollToItem(visibleMessages.lastIndex)
        }
    }

    // Separate pickers for Image, Video, and Document attachments
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val caption = composerValue.text
            composerValue = TextFieldValue("")
            viewModel.uploadAndSendAttachment(
                conversationId = conversationId,
                uri = uri,
                caption = caption,
                fallbackMime = "image/jpeg"
            )
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val caption = composerValue.text
            composerValue = TextFieldValue("")
            viewModel.uploadAndSendAttachment(
                conversationId = conversationId,
                uri = uri,
                caption = caption,
                fallbackMime = "video/mp4"
            )
        }
    }

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val caption = composerValue.text
            composerValue = TextFieldValue("")
            viewModel.uploadAndSendAttachment(
                conversationId = conversationId,
                uri = uri,
                caption = caption,
                fallbackMime = "application/octet-stream"
            )
        }
    }

    if (showEmojiSheet) {
        EmojiPickerSheet(
            onEmojiSelected = { emoji ->
                val currentText = composerValue.text
                val selection = composerValue.selection
                val start = selection.min.coerceIn(0, currentText.length)
                val end = selection.max.coerceIn(0, currentText.length)
                val newText = buildString {
                    append(currentText.substring(0, start))
                    append(emoji)
                    append(currentText.substring(end))
                }
                val nextCursor = start + emoji.length
                composerValue = TextFieldValue(
                    text = newText,
                    selection = TextRange(nextCursor)
                )
            },
            onDismiss = { showEmojiSheet = false }
        )
    }

    if (showInfoSheet) {
        ConversationInfoSheet(
            conversation = conversation,
            messages = allMessages,
            currentUsername = uiState.currentUser?.username,
            groupMembers = groupMembers,
            friendIds = friendIdSet,
            resolveAssetUrl = viewModel::resolveAssetUrl,
            onPreviewImage = { url ->
                showInfoSheet = false
                previewImageUrl = url
            },
            onUnfriend = conversation.targetUserId?.let { targetId ->
                {
                    viewModel.unfriendUser(
                        targetUserId = targetId,
                        displayName = conversation.name?.takeIf { it.isNotBlank() } ?: "người dùng"
                    )
                }
            },
            onReFriend = conversation.targetUserId?.let { targetId ->
                {
                    viewModel.sendFriendRequestById(
                        targetUserId = targetId,
                        displayName = conversation.name?.takeIf { it.isNotBlank() } ?: "người dùng"
                    )
                }
            },
            onMessageMember = { member ->
                showInfoSheet = false
                viewModel.openOrCreatePrivateChat(
                    targetUserId = member.id,
                    fallbackName = member.displayName,
                    fallbackUsername = member.username,
                    fallbackAvatarUrl = member.avatarUrl,
                    fallbackOnline = member.isOnline,
                    onReady = { nextId ->
                        if (nextId != conversationId) {
                            viewModel.openConversation(nextId)
                        }
                    }
                )
            },
            onAddFriendMember = { member ->
                viewModel.sendFriendRequestById(
                    targetUserId = member.id,
                    displayName = member.displayName
                )
            },
            onDeleteConversation = {
                showInfoSheet = false
                viewModel.deleteConversation(conversationId = conversationId) {
                    onNavigateBack()
                }
            },
            onDismiss = { showInfoSheet = false }
        )
    }

    if (pendingDeleteIds.isNotEmpty()) {
        val currentUsername = uiState.currentUser?.username
        val targetMessages = remember(pendingDeleteIds, allMessages) {
            val idSet = pendingDeleteIds.toHashSet()
            allMessages.filter { it.id in idSet }
        }
        val allPendingMine = targetMessages.isNotEmpty() && targetMessages.all { msg ->
            msg.sender.username.equals(currentUsername, ignoreCase = true)
        }
        val canDeleteForEveryone = !conversation.isGroup ||
            conversation.isCurrentUserGroupAdmin ||
            allPendingMine

        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeletingMessages) {
                    pendingDeleteIds = emptyList()
                }
            },
            title = {
                Text(
                    text = if (pendingDeleteIds.size == 1) {
                        "Xóa tin nhắn"
                    } else {
                        "Xóa ${pendingDeleteIds.size} tin nhắn đã chọn"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (canDeleteForEveryone) {
                            "Chọn cách xóa tin nhắn trong cuộc trò chuyện này:"
                        } else {
                            "Trong nhóm trò chuyện, bạn chỉ có thể thu hồi tin nhắn của chính mình (hoặc khi là quản trị viên). Bạn vẫn có thể xóa ở phía mình:"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (canDeleteForEveryone) {
                        Button(
                            onClick = {
                                val idsToDelete = pendingDeleteIds
                                viewModel.deleteMessages(
                                    conversationId = conversationId,
                                    messageIds = idsToDelete,
                                    mode = "FOR_EVERYONE"
                                ) {
                                    pendingDeleteIds = emptyList()
                                    selectedMessageIds = emptySet()
                                    isSelectionMode = false
                                }
                            },
                            enabled = !uiState.isDeletingMessages,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thu hồi / Xóa với mọi người")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val idsToDelete = pendingDeleteIds
                            viewModel.deleteMessages(
                                conversationId = conversationId,
                                messageIds = idsToDelete,
                                mode = "FOR_ME"
                            ) {
                                pendingDeleteIds = emptyList()
                                selectedMessageIds = emptySet()
                                isSelectionMode = false
                            }
                        },
                        enabled = !uiState.isDeletingMessages,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xóa ở phía tôi")
                    }

                    if (!isSelectionMode && pendingDeleteIds.size == 1) {
                        OutlinedButton(
                            onClick = {
                                val seedId = pendingDeleteIds.first()
                                pendingDeleteIds = emptyList()
                                isSelectionMode = true
                                selectedMessageIds = setOf(seedId)
                            },
                            enabled = !uiState.isDeletingMessages,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chọn nhiều tin nhắn")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteIds = emptyList() },
                    enabled = !uiState.isDeletingMessages
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    previewImageUrl?.let { imageUrl ->
        Dialog(
            onDismissRequest = { previewImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { previewImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Xem ảnh phóng to",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { previewImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Đóng",
                        tint = Color.White
                    )
                }
            }
        }
    }

    val roomTitle = conversation.name?.takeIf { it.isNotBlank() } ?: "Hội thoại #$conversationId"
    val roomSubtitle = if (conversation.isGroup) {
        val count = groupMembers.size.takeIf { it > 0 } ?: conversation.memberCount ?: 0
        if (count > 0) "$count thành viên" else "Nhóm trò chuyện"
    } else {
        formatPresenceLabel(conversation.isOnline, conversation.lastSeen, conversation.presenceStatus)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Quay lại"
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showInfoSheet = true }
                        ) {
                            AvatarView(
                                displayName = roomTitle,
                                avatarUrl = viewModel.resolveAssetUrl(conversation.avatarUrl),
                                size = 40.dp,
                                isOnline = conversation.isOnline == true,
                                presenceStatus = conversation.presenceStatus,
                                showOnlineBadge = !conversation.isGroup,
                                isGroup = conversation.isGroup
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = roomTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = roomSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (conversation.isOnline == true && !conversation.isGroup) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        if (allMessages.isNotEmpty() || isSelectionMode) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = !isSelectionMode
                                    selectedMessageIds = emptySet()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = "Chọn nhiều tin nhắn",
                                    tint = if (isSelectionMode) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) viewModel.setInChatSearchQuery("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Tìm trong cuộc trò chuyện"
                            )
                        }
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Thông tin hội thoại"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (showSearchBar) {
                    OutlinedTextField(
                        value = uiState.inChatSearchQuery,
                        onValueChange = viewModel::setInChatSearchQuery,
                        placeholder = { Text("Tìm nội dung tin nhắn hoặc tên tệp...") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (uiState.inChatSearchQuery.isNotEmpty()) {
                                        viewModel.setInChatSearchQuery("")
                                    } else {
                                        showSearchBar = false
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Đóng")
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                if (uiState.isUploadingAttachment) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                val visibleIds = remember(visibleMessages) { visibleMessages.map { it.id } }
                val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedMessageIds }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                selectedMessageIds = if (allVisibleSelected) {
                                    selectedMessageIds - visibleIds.toSet()
                                } else {
                                    selectedMessageIds + visibleIds
                                }
                            },
                            enabled = visibleIds.isNotEmpty()
                        ) {
                            Text(if (allVisibleSelected) "Bỏ chọn" else "Chọn tất cả")
                        }

                        Text(
                            text = "Đã chọn ${selectedMessageIds.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                isSelectionMode = false
                                selectedMessageIds = emptySet()
                            }
                        ) {
                            Text("Hủy")
                        }

                        Button(
                            onClick = {
                                if (selectedMessageIds.isNotEmpty()) {
                                    pendingDeleteIds = selectedMessageIds.toList()
                                }
                            },
                            enabled = selectedMessageIds.isNotEmpty() && !uiState.isDeletingMessages,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xóa (${selectedMessageIds.size})")
                        }
                    }
                }
            } else if (conversation.isUnfriendedPrivate) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Hai bạn hiện không còn là bạn bè. Kết bạn lại để tiếp tục gửi tin nhắn.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        val targetId = conversation.targetUserId
                        if (conversation.friendshipStatus.equals("PENDING", ignoreCase = true)) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Đã gửi")
                            }
                        } else if (targetId != null) {
                            Button(
                                onClick = {
                                    viewModel.sendFriendRequestById(
                                        targetUserId = targetId,
                                        displayName = roomTitle
                                    )
                                },
                                enabled = targetId !in uiState.sendingFriendRequestUserIds,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kết bạn lại")
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box {
                            IconButton(
                                onClick = { showAttachmentMenu = true },
                                enabled = !uiState.isUploadingAttachment
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = "Đính kèm",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            DropdownMenu(
                                expanded = showAttachmentMenu,
                                onDismissRequest = { showAttachmentMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Chọn gửi ảnh") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Image,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showAttachmentMenu = false
                                        pickImageLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Chọn gửi video") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Videocam,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showAttachmentMenu = false
                                        pickVideoLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.VideoOnly
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Đính kèm tệp") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.AttachFile,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showAttachmentMenu = false
                                        pickDocumentLauncher.launch(arrayOf("*/*"))
                                    }
                                )
                            }
                        }

                        IconButton(onClick = { showEmojiSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.SentimentSatisfiedAlt,
                                contentDescription = "Chọn emoji",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = composerValue,
                            onValueChange = { composerValue = it },
                            placeholder = { Text("Nhập tin nhắn...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(22.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    val textToSend = composerValue.text
                                    if (textToSend.isNotBlank() && !uiState.isSendingMessage) {
                                        composerValue = TextFieldValue("")
                                        viewModel.sendTextMessage(
                                            conversationId = conversationId,
                                            rawText = textToSend
                                        )
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        val canSend = composerValue.text.isNotBlank() && !uiState.isSendingMessage
                        FilledIconButton(
                            onClick = {
                                val textToSend = composerValue.text
                                if (textToSend.isNotBlank()) {
                                    composerValue = TextFieldValue("")
                                    viewModel.sendTextMessage(
                                        conversationId = conversationId,
                                        rawText = textToSend
                                    )
                                }
                            },
                            enabled = canSend
                        ) {
                            if (uiState.isSendingMessage) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = "Gửi tin nhắn"
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isMessagesLoading && allMessages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                visibleMessages.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showSearchBar && uiState.inChatSearchQuery.isNotBlank()) {
                                "Không tìm thấy tin nhắn nào khớp từ khóa."
                            } else {
                                "Hãy gửi lời chào đầu tiên để bắt đầu cuộc trò chuyện."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (hasMore && !showSearchBar) {
                            item(key = "load_older_button") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadOlderMessages(conversationId) },
                                        enabled = !uiState.isLoadingOlderMessages,
                                        shape = RoundedCornerShape(18.dp)
                                    ) {
                                        if (uiState.isLoadingOlderMessages) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("Tải tin nhắn cũ hơn")
                                    }
                                }
                            }
                        }

                        items(
                            items = visibleMessages,
                            key = { it.id }
                        ) { message ->
                            val isMe = message.sender.username.equals(
                                uiState.currentUser?.username,
                                ignoreCase = true
                            )
                            val isSelected = message.id in selectedMessageIds
                            MessageBubbleItem(
                                message = message,
                                isMe = isMe,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    selectedMessageIds = if (isSelected) {
                                        selectedMessageIds - message.id
                                    } else {
                                        selectedMessageIds + message.id
                                    }
                                },
                                onLongPressMessage = {
                                    pendingDeleteIds = listOf(message.id)
                                },
                                resolveAssetUrl = viewModel::resolveAssetUrl,
                                onPreviewImage = { previewImageUrl = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleItem(
    message: MessageDto,
    isMe: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongPressMessage: () -> Unit,
    resolveAssetUrl: (String?) -> String?,
    onPreviewImage: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val senderLabel = if (isMe) {
        "Bạn"
    } else {
        message.sender.fullName?.takeIf { it.isNotBlank() } ?: message.sender.username
    }

    val attachmentType = message.attachmentType?.uppercase().orEmpty()
    val resolvedAttachmentUrl = resolveAssetUrl(message.attachmentUrl)
    val autoPlaceholderText = when (attachmentType) {
        "IMAGE" -> "[Hình ảnh] ${message.attachmentName.orEmpty()}".trim()
        "VIDEO" -> "[Video] ${message.attachmentName.orEmpty()}".trim()
        "FILE" -> "[Tệp] ${message.attachmentName.orEmpty()}".trim()
        else -> ""
    }
    val hasCustomBodyText = message.content.isNotBlank() &&
        (resolvedAttachmentUrl == null || message.content.trim() != autoPlaceholderText)

    val detectedLink = remember(message.content, attachmentType, message.attachmentUrl) {
        if (attachmentType == "LINK" && !message.attachmentUrl.isNullOrBlank()) {
            val url = message.attachmentUrl
            val host = extractHostName(url)
            Triple(url, message.attachmentName?.takeIf { it.isNotBlank() } ?: host, host)
        } else {
            extractFirstLink(message.content)?.let { Triple(it.url, it.label, it.host) }
        }
    }

    val bubbleContainerColor = if (isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val bubbleTextColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val bubbleLinkColor = if (isMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    val rowBackground = if (isSelectionMode && isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBackground)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    }
                },
                onLongClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onLongPressMessage()
                    }
                }
            )
            .padding(
                horizontal = if (isSelectionMode) 6.dp else 0.dp,
                vertical = if (isSelectionMode) 4.dp else 0.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = if (isSelected) "Đã chọn" else "Chưa chọn",
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMe) {
                AvatarView(
                    displayName = senderLabel,
                    avatarUrl = resolveAssetUrl(message.sender.avatarUrl),
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = if (isSelectionMode) 268.dp else 300.dp)
            ) {
                // Sender name header shown cleanly for both group and private conversations
                Text(
                    text = senderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Surface(
                    color = bubbleContainerColor,
                    contentColor = bubbleTextColor,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    ),
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelect()
                            }
                        },
                        onLongClick = {
                            if (isSelectionMode) {
                                onToggleSelect()
                            } else {
                                onLongPressMessage()
                            }
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (attachmentType == "IMAGE" && !resolvedAttachmentUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = resolvedAttachmentUrl,
                                contentDescription = message.attachmentName ?: "Hình ảnh",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                onToggleSelect()
                                            } else {
                                                onPreviewImage(resolvedAttachmentUrl)
                                            }
                                        },
                                        onLongClick = {
                                            if (isSelectionMode) {
                                                onToggleSelect()
                                            } else {
                                                onLongPressMessage()
                                            }
                                        }
                                    )
                            )
                        }

                        if (attachmentType == "VIDEO" && !resolvedAttachmentUrl.isNullOrBlank()) {
                            AttachmentActionRow(
                                icon = Icons.Outlined.PlayCircleOutline,
                                title = message.attachmentName?.takeIf { it.isNotBlank() }
                                    ?: "Video đính kèm",
                                subtitle = listOfNotNull(
                                    "Nhấn để phát video",
                                    message.attachmentSize?.takeIf { it.isNotBlank() }
                                ).joinToString(" · "),
                                isMe = isMe,
                                onClick = {
                                    if (isSelectionMode) {
                                        onToggleSelect()
                                    } else {
                                        runCatching { uriHandler.openUri(resolvedAttachmentUrl) }
                                    }
                                },
                                onLongClick = {
                                    if (isSelectionMode) {
                                        onToggleSelect()
                                    } else {
                                        onLongPressMessage()
                                    }
                                }
                            )
                        }

                        if (attachmentType == "FILE" && !resolvedAttachmentUrl.isNullOrBlank()) {
                            AttachmentActionRow(
                                icon = Icons.Outlined.Description,
                                title = message.attachmentName?.takeIf { it.isNotBlank() }
                                    ?: "Tệp đính kèm",
                                subtitle = message.attachmentSize?.takeIf { it.isNotBlank() }
                                    ?: "Nhấn để mở hoặc tải tệp",
                                isMe = isMe,
                                onClick = {
                                    if (isSelectionMode) {
                                        onToggleSelect()
                                    } else {
                                        runCatching { uriHandler.openUri(resolvedAttachmentUrl) }
                                    }
                                },
                                onLongClick = {
                                    if (isSelectionMode) {
                                        onToggleSelect()
                                    } else {
                                        onLongPressMessage()
                                    }
                                }
                            )
                        }

                        if (hasCustomBodyText) {
                            if (isSelectionMode) {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = bubbleTextColor
                                )
                            } else {
                                MarkdownLinkText(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textColor = bubbleTextColor,
                                    linkColor = bubbleLinkColor
                                )
                            }
                        }

                        if (detectedLink != null && attachmentType != "IMAGE" && attachmentType != "VIDEO" && attachmentType != "FILE") {
                            val (url, label, host) = detectedLink
                            AttachmentActionRow(
                                icon = Icons.Outlined.Link,
                                title = label,
                                subtitle = host,
                                isMe = isMe,
                                onClick = {
                                    if (isSelectionMode) {
                                        onToggleSelect()
                                    } else {
                                        runCatching { uriHandler.openUri(url) }
                                    }
                                },
                                onLongClick = {
                                    if (isSelectionMode) {
                                        onToggleSelect()
                                    } else {
                                        onLongPressMessage()
                                    }
                                }
                            )
                        }
                    }
                }

                val timeText = formatMessageTime(message.createdAt)
                if (timeText.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isSelectionMode) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Xóa tin nhắn",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .clickable { onLongPressMessage() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttachmentActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isMe: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val containerBg = if (isMe) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val fgColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = fgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            tint = subColor,
            modifier = Modifier.size(16.dp)
        )
    }
}
