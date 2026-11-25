package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.model.UserSearchResponse
import nhom3.chatbln.android.ui.components.AlertBannerTone
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.components.InlineAlertBanner
import nhom3.chatbln.android.ui.components.TonalListSkeleton
import nhom3.chatbln.android.ui.components.resolvePeerPresenceIcon
import nhom3.chatbln.android.ui.viewmodel.UserRelationshipState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendBottomSheet(
    keyword: String,
    hasSearched: Boolean,
    isSearching: Boolean,
    results: List<UserSearchResponse>,
    sendingUserIds: Set<Long>,
    feedbackMessage: String? = null,
    isFeedbackError: Boolean = false,
    resolveAssetUrl: (String?) -> String?,
    resolveRelationship: (UserSearchResponse) -> UserRelationshipState,
    onKeywordChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSendRequest: (UserSearchResponse) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val trimmedKeyword = keyword.trim().trimStart('@')

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
                text = "Tìm và kết bạn",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )

            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                placeholder = { Text("Nhập họ tên hoặc @username...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(
                            onClick = { onKeywordChange("") },
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSearchSubmit()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (!feedbackMessage.isNullOrBlank()) {
                InlineAlertBanner(
                    message = feedbackMessage,
                    tone = if (isFeedbackError) AlertBannerTone.ERROR else AlertBannerTone.SUCCESS
                )
            }

            // Explicitly hide system users until a non-blank search keyword is entered
            when {
                trimmedKeyword.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nhập từ khóa để tìm người dùng",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Danh sách người dùng hệ thống được ẩn cho đến khi bạn nhập tên hoặc @username.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                isSearching -> {
                    TonalListSkeleton(
                        rowCount = 4,
                        showTrailingAction = true
                    )
                }

                hasSearched && results.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy người dùng nào với từ khóa \"$trimmedKeyword\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(
                            items = results,
                            key = { it.id },
                            contentType = { "user_search_row" }
                        ) { user ->
                            val displayName = user.fullName?.takeIf { it.isNotBlank() }
                                ?: user.username
                            val relationship = resolveRelationship(user)
                            val isSending = sendingUserIds.contains(user.id)
                            val (peerIcon, peerColor) = resolvePeerPresenceIcon(
                                isOnline = user.isOnline,
                                presenceStatus = user.presenceStatus
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarView(
                                    displayName = displayName,
                                    avatarUrl = resolveAssetUrl(user.avatarUrl),
                                    size = 44.dp,
                                    isOnline = user.isOnline == true,
                                    presenceStatus = user.presenceStatus,
                                    showOnlineBadge = true
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = peerIcon,
                                            contentDescription = null,
                                            tint = peerColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                when (relationship) {
                                    UserRelationshipState.SELF -> {
                                        Text(
                                            text = "Bạn",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    UserRelationshipState.FRIEND -> {
                                        OutlinedButton(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Bạn bè")
                                        }
                                    }

                                    UserRelationshipState.SENT_PENDING -> {
                                        OutlinedButton(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Đã gửi")
                                        }
                                    }

                                    UserRelationshipState.RECEIVED_PENDING -> {
                                        OutlinedButton(
                                            onClick = {},
                                            enabled = false,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Text("Chờ bạn duyệt")
                                        }
                                    }

                                    UserRelationshipState.NONE -> {
                                        Button(
                                            onClick = { onSendRequest(user) },
                                            enabled = !isSending,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            if (isSending) {
                                                CircularProgressIndicator(
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Text("Kết bạn")
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
            }
        }
    }
}
