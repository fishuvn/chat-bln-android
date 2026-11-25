package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.model.FriendResponse
import nhom3.chatbln.android.ui.components.AvatarView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatBottomSheet(
    friends: List<FriendResponse>,
    isSubmitting: Boolean,
    resolveAssetUrl: (String?) -> String?,
    onDismiss: () -> Unit,
    onSelectFriendForPrivateChat: (FriendResponse) -> Unit,
    onCreateGroup: (groupName: String, memberIds: List<Long>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isGroupMode by rememberSaveable { mutableStateOf(false) }
    var groupName by rememberSaveable { mutableStateOf("") }
    var filterQuery by rememberSaveable { mutableStateOf("") }
    val selectedMemberIds = remember { mutableStateListOf<Long>() }

    val filteredFriends = remember(friends, filterQuery) {
        val q = filterQuery.trim()
        if (q.isEmpty()) {
            friends
        } else {
            friends.filter { f ->
                (f.fullName.orEmpty()).contains(q, ignoreCase = true) ||
                    f.username.contains(q, ignoreCase = true)
            }
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isGroupMode) "Tạo nhóm trò chuyện mới" else "Bắt đầu cuộc trò chuyện",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isGroupMode,
                    onClick = { isGroupMode = false },
                    label = { Text("Trò chuyện 1-1") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                    }
                )
                FilterChip(
                    selected = isGroupMode,
                    onClick = { isGroupMode = true },
                    label = { Text("Tạo nhóm") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Groups, contentDescription = null)
                    }
                )
            }

            if (isGroupMode) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Tên nhóm *") },
                    placeholder = { Text("Ví dụ: Nhóm Đồ Án Android") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Lọc bạn bè theo tên hoặc @username...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (friends.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Danh bạ bạn bè đang trống",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hãy chuyển sang mục Danh bạ để tìm và kết bạn trước.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(items = filteredFriends, key = { it.id }) { friend ->
                        val isChecked = selectedMemberIds.contains(friend.id)
                        val displayName = friend.fullName?.takeIf { it.isNotBlank() } ?: friend.username

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSubmitting) {
                                    if (isGroupMode) {
                                        if (isChecked) {
                                            selectedMemberIds.remove(friend.id)
                                        } else {
                                            selectedMemberIds.add(friend.id)
                                        }
                                    } else {
                                        onSelectFriendForPrivateChat(friend)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                displayName = displayName,
                                avatarUrl = resolveAssetUrl(friend.avatarUrl),
                                size = 44.dp,
                                isOnline = friend.isOnline,
                                presenceStatus = friend.presenceStatus,
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
                                Text(
                                    text = "@${friend.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isGroupMode) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!selectedMemberIds.contains(friend.id)) {
                                                selectedMemberIds.add(friend.id)
                                            }
                                        } else {
                                            selectedMemberIds.remove(friend.id)
                                        }
                                    }
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            if (isGroupMode) {
                Button(
                    onClick = {
                        onCreateGroup(groupName.trim(), selectedMemberIds.toList())
                    },
                    enabled = !isSubmitting &&
                        groupName.trim().isNotEmpty() &&
                        selectedMemberIds.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Tạo nhóm (${selectedMemberIds.size} thành viên)")
                }
            }
        }
    }
}
