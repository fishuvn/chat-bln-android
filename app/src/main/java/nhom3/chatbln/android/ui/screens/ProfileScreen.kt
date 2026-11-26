package nhom3.chatbln.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import nhom3.chatbln.android.BuildConfig
import nhom3.chatbln.android.data.remote.AppUpdateChecker
import nhom3.chatbln.android.data.remote.AppUpdateInfo
import nhom3.chatbln.android.ui.components.UpdateCheckerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.local.UserPresenceMode
import nhom3.chatbln.android.ui.components.AvatarView
import nhom3.chatbln.android.ui.components.ServerUrlDialog
import nhom3.chatbln.android.ui.components.resolvePresenceVisual
import nhom3.chatbln.android.ui.theme.isDynamicColorSupported
import nhom3.chatbln.android.ui.viewmodel.MainChatUiState
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    uiState: MainChatUiState,
    isDarkThemeEffective: Boolean,
    resolveAssetUrl: (String?) -> String?,
    onUploadAvatar: (Uri) -> Unit,
    onSaveProfile: (
        fullName: String,
        email: String,
        phone: String,
        gender: String,
        dob: String,
        bio: String,
        avatarUrl: String?
    ) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    onChangePresenceMode: (UserPresenceMode) -> Unit,
    onUpdateServerUrl: (String) -> Unit,
    onLogout: () -> Unit
) {
    val profile = uiState.profile
    val fallback = uiState.currentUser

    val sourceKey = "${profile?.id ?: fallback?.id}_${profile?.avatarUrl}_${profile?.fullName}_${profile?.email}_${profile?.phone}_${profile?.gender}_${profile?.dob}_${profile?.bio}"

    var fullName by rememberSaveable(sourceKey) {
        mutableStateOf(profile?.fullName ?: fallback?.fullName.orEmpty())
    }
    var email by rememberSaveable(sourceKey) {
        mutableStateOf(profile?.email ?: fallback?.email.orEmpty())
    }
    var phone by rememberSaveable(sourceKey) {
        mutableStateOf(profile?.phone ?: fallback?.phone.orEmpty())
    }
    var gender by rememberSaveable(sourceKey) {
        mutableStateOf(profile?.gender ?: fallback?.gender ?: "Nam")
    }
    var dob by rememberSaveable(sourceKey) {
        mutableStateOf(profile?.dob ?: fallback?.dob.orEmpty())
    }
    var bio by rememberSaveable(sourceKey) {
        mutableStateOf(profile?.bio ?: fallback?.bio.orEmpty())
    }

    LaunchedEffect(profile) {
        if (profile != null) {
            fullName = profile.fullName.orEmpty()
            email = profile.email.orEmpty()
            phone = profile.phone.orEmpty()
            gender = profile.gender ?: "Nam"
            dob = profile.dob.orEmpty()
            bio = profile.bio.orEmpty()
        }
    }

    var showServerDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusText by remember {
        mutableStateOf("Phiên bản v${BuildConfig.VERSION_NAME} (${AppUpdateChecker.primaryAbiLabel()})")
    }
    var updateDialogInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()

    updateDialogInfo?.let { info ->
        UpdateCheckerDialog(
            info = info,
            onDismiss = { updateDialogInfo = null }
        )
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadAvatar(uri)
        }
    }

    if (showServerDialog) {
        ServerUrlDialog(
            currentUrl = uiState.serverBaseUrl,
            onDismiss = { showServerDialog = false },
            onSave = { updated ->
                onUpdateServerUrl(updated)
                showServerDialog = false
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dob = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toString()
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Đăng xuất tài khoản") },
            text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi thiết bị này không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Đăng xuất", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirm = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    val username = profile?.username ?: fallback?.username.orEmpty()
    val rawAvatarUrl = profile?.avatarUrl ?: fallback?.avatarUrl
    val genderOptions = listOf("Nam", "Nữ", "Khác")
    val supportsDynamicColor = isDynamicColorSupported()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hồ sơ & Cài đặt",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AvatarView(
                        displayName = fullName.ifBlank { username.ifBlank { "U" } },
                        avatarUrl = resolveAssetUrl(rawAvatarUrl),
                        size = 76.dp,
                        isOnline = uiState.presenceMode != UserPresenceMode.OFFLINE,
                        presenceStatus = uiState.presenceMode.rawValue,
                        showOnlineBadge = true
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = fullName.ifBlank { username },
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (username.isNotBlank()) {
                            Text(
                                text = "@$username",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            enabled = !uiState.isUploadingAvatar,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            if (uiState.isUploadingAvatar) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang tải ảnh...")
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đổi ảnh đại diện")
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Trạng thái hoạt động",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UserPresenceMode.entries.forEach { mode ->
                            val visual = resolvePresenceVisual(mode = mode, isWsConnected = true)
                            FilterChip(
                                selected = uiState.presenceMode == mode,
                                onClick = { onChangePresenceMode(mode) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = visual.icon,
                                        contentDescription = null,
                                        tint = visual.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(mode.shortTitle) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                Text(
                    text = "Thông tin cá nhân",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên hiển thị") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Giới tính",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        genderOptions.forEach { option ->
                            FilterChip(
                                selected = gender == option,
                                onClick = { gender = option },
                                label = { Text(option) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Ngày sinh (YYYY-MM-DD)") },
                    placeholder = { Text("2003-06-18") },
                    trailingIcon = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = "Chọn ngày sinh"
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Tiểu sử") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onSaveProfile(
                            fullName,
                            email,
                            phone,
                            gender,
                            dob,
                            bio,
                            rawAvatarUrl
                        )
                    },
                    enabled = !uiState.isSavingProfile,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    if (uiState.isSavingProfile) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Lưu thay đổi hồ sơ")
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                Text(
                    text = "Giao diện & Kết nối",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDarkMode(!isDarkThemeEffective) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chế độ tối",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (isDarkThemeEffective) {
                                        "Đang dùng nền tối dịu mắt"
                                    } else {
                                        "Đang dùng nền sáng tiêu chuẩn"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isDarkThemeEffective,
                                onCheckedChange = onToggleDarkMode
                            )
                        }

                        if (supportsDynamicColor) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleDynamicColor(!uiState.dynamicColorEnabled) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Màu động Material You",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (uiState.dynamicColorEnabled) {
                                            "Đồng bộ bảng màu theo hình nền Android 12+"
                                        } else {
                                            "Bảng màu Sapphire Quiet Signal mặc định"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.dynamicColorEnabled,
                                    onCheckedChange = onToggleDynamicColor
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showServerDialog = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Địa chỉ máy chủ Backend",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = uiState.serverBaseUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { showServerDialog = true },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Đổi")
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isCheckingUpdate) {
                                    isCheckingUpdate = true
                                    updateStatusText = "Đang kiểm tra bản phát hành mới..."
                                    coroutineScope.launch {
                                        AppUpdateChecker.checkForUpdates()
                                            .onSuccess { info ->
                                                updateStatusText = if (info.hasUpdate) {
                                                    "Có bản mới ${info.latestTag} (hiện tại v${info.currentVersion})"
                                                } else {
                                                    "Đang dùng bản mới nhất (v${info.currentVersion})"
                                                }
                                                updateDialogInfo = info
                                            }
                                            .onFailure { err ->
                                                updateStatusText = err.message ?: "Không thể kiểm tra cập nhật."
                                            }
                                        isCheckingUpdate = false
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kiểm tra cập nhật",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = updateStatusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        isCheckingUpdate = true
                                        updateStatusText = "Đang kiểm tra bản phát hành mới..."
                                        coroutineScope.launch {
                                            AppUpdateChecker.checkForUpdates()
                                                .onSuccess { info ->
                                                    updateStatusText = if (info.hasUpdate) {
                                                        "Có bản mới ${info.latestTag} (hiện tại v${info.currentVersion})"
                                                    } else {
                                                        "Đang dùng bản mới nhất (v${info.currentVersion})"
                                                    }
                                                    updateDialogInfo = info
                                                }
                                                .onFailure { err ->
                                                    updateStatusText = err.message ?: "Không thể kiểm tra cập nhật."
                                                }
                                            isCheckingUpdate = false
                                        }
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Kiểm tra")
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showLogoutConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng xuất")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
