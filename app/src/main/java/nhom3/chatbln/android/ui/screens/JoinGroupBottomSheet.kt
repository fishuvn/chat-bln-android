package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.model.GroupInvitePreviewResponse
import nhom3.chatbln.android.ui.components.AlertBannerTone
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.components.InlineAlertBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupBottomSheet(
    initialInput: String = "",
    preview: GroupInvitePreviewResponse?,
    isLoadingPreview: Boolean,
    previewError: String?,
    isJoining: Boolean,
    feedbackMessage: String?,
    feedbackIsError: Boolean,
    onPreview: (String) -> Unit,
    onJoin: (String) -> Unit,
    onOpenExistingGroup: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    var inviteInput by rememberSaveable(initialInput) { mutableStateOf(initialInput) }

    LaunchedEffect(initialInput) {
        val clean = initialInput.trim()
        if (clean.isNotEmpty()) {
            onPreview(clean)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tham gia nhóm bằng link / mã mời",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            Text(
                text = "Dán liên kết mời (ví dụ: https://chat-bln.fishu.io.vn/join/grp_xxx) hoặc nhập trực tiếp mã mời grp_xxx.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = inviteInput,
                onValueChange = { inviteInput = it },
                label = { Text("Liên kết hoặc mã mời nhóm") },
                placeholder = { Text("grp_hhf3wid0rp") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Link, contentDescription = null)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (inviteInput.isNotEmpty()) {
                            IconButton(
                                onClick = { inviteInput = "" },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Xóa mã mời"
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                    if (pasted.isNotEmpty()) {
                                        inviteInput = pasted
                                        onPreview(pasted)
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = "Dán từ bộ nhớ tạm"
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onPreview(inviteInput)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    onPreview(inviteInput)
                },
                enabled = inviteInput.isNotBlank() && !isLoadingPreview && !isJoining,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                if (isLoadingPreview) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang kiểm tra thông tin nhóm...")
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xem thông tin nhóm")
                }
            }

            if (!previewError.isNullOrBlank()) {
                InlineAlertBanner(
                    message = previewError,
                    tone = AlertBannerTone.ERROR
                )
            }

            if (!feedbackMessage.isNullOrBlank()) {
                InlineAlertBanner(
                    message = feedbackMessage,
                    tone = if (feedbackIsError) AlertBannerTone.ERROR else AlertBannerTone.SUCCESS
                )
            }

            if (preview != null) {
                val groupTitle = preview.name?.takeIf { it.isNotBlank() }
                    ?: "Nhóm #${preview.conversationId}"
                val isPrivate = preview.isPrivateGroup
                val isMember = preview.alreadyMember ||
                    preview.joinStatus.equals("MEMBER", ignoreCase = true)
                val isPending = preview.joinStatus.equals("PENDING", ignoreCase = true)

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarView(
                                displayName = groupTitle,
                                avatarUrl = null,
                                size = 52.dp,
                                isGroup = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = groupTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = if (isPrivate) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        },
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPrivate) {
                                                    Icons.Outlined.Lock
                                                } else {
                                                    Icons.Outlined.Public
                                                },
                                                contentDescription = null,
                                                tint = if (isPrivate) {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                },
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (isPrivate) "Nhóm riêng tư" else "Nhóm công khai",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isPrivate) {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                }
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${preview.memberCount} thành viên",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = when {
                                isMember -> "Bạn đã là thành viên của nhóm này."
                                isPending -> "Yêu cầu tham gia của bạn đang chờ Trưởng nhóm hoặc Phó nhóm phê duyệt."
                                isPrivate -> "Nhóm riêng tư yêu cầu Trưởng nhóm hoặc Phó nhóm xác nhận trước khi vào nhóm."
                                else -> "Nhóm công khai cho phép bạn tham gia trò chuyện ngay lập tức."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        when {
                            isMember -> {
                                Button(
                                    onClick = { onOpenExistingGroup(preview.conversationId) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Mở nhóm trò chuyện")
                                }
                            }

                            isPending -> {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Đang chờ phê duyệt")
                                }
                            }

                            else -> {
                                Button(
                                    onClick = { onJoin(preview.inviteCode) },
                                    enabled = !isJoining,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    if (isJoining) {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            if (isPrivate) {
                                                "Gửi yêu cầu tham gia"
                                            } else {
                                                "Tham gia nhóm ngay"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
