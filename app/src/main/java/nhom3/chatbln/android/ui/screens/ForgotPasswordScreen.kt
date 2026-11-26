package nhom3.chatbln.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import nhom3.chatbln.android.ui.components.AlertBannerTone
import nhom3.chatbln.android.ui.components.InlineAlertBanner
import nhom3.chatbln.android.ui.components.ServerUrlDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    currentStep: Int,
    savedEmail: String,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    generatedOtpHint: String?,
    serverBaseUrl: String,
    onSendOtp: (email: String, onOtpIssued: () -> Unit) -> Unit,
    onVerifyOtp: (email: String, otp: String) -> Unit,
    onResetPassword: (
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String
    ) -> Unit,
    onStepBack: () -> Unit,
    onUpdateServerUrl: (String) -> Unit,
    onClearError: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf(savedEmail) }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showServerDialog by rememberSaveable { mutableStateOf(false) }
    var resendCountdown by rememberSaveable { mutableIntStateOf(0) }

    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onStepBack)

    LaunchedEffect(savedEmail) {
        if (savedEmail.isNotBlank() && email.isBlank()) {
            email = savedEmail
        }
    }

    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1_000L)
            resendCountdown -= 1
        }
    }

    if (showServerDialog) {
        ServerUrlDialog(
            currentUrl = serverBaseUrl,
            onDismiss = { showServerDialog = false },
            onSave = { updated ->
                onUpdateServerUrl(updated)
                showServerDialog = false
            }
        )
    }

    val stepTitle = when (currentStep) {
        1 -> "Khôi phục mật khẩu"
        2 -> "Xác minh mã OTP"
        else -> "Đặt mật khẩu mới"
    }

    val stepSubtitle = when (currentStep) {
        1 -> "Nhập địa chỉ email đã liên kết với tài khoản Chat BLN để nhận mã xác thực 6 chữ số."
        2 -> "Nhập mã OTP 6 chữ số vừa được cấp cho ${email.ifBlank { savedEmail }}."
        else -> "Thiết lập mật khẩu mới an toàn (từ 6 đến 72 ký tự) cho tài khoản của bạn."
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Bước $currentStep / 3",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onStepBack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Quay lại"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showServerDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = "Cấu hình máy chủ"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                LinearProgressIndicator(
                    progress = { currentStep.coerceIn(1, 3) / 3f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stepTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = stepSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!infoMessage.isNullOrBlank()) {
                    InlineAlertBanner(
                        message = infoMessage,
                        tone = AlertBannerTone.INFO
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    InlineAlertBanner(
                        message = errorMessage,
                        tone = AlertBannerTone.ERROR
                    )
                }

                when (currentStep) {
                    1 -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (errorMessage != null) onClearError()
                            },
                            label = { Text("Email đã đăng ký") },
                            placeholder = { Text("ten@domain.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.MailOutline,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSendOtp(email) { resendCountdown = 60 }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onSendOtp(email) { resendCountdown = 60 }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = "Nhận mã xác thực OTP",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    2 -> {
                        if (!generatedOtpHint.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = {
                                        otpCode = generatedOtpHint
                                        if (errorMessage != null) onClearError()
                                    },
                                    label = { Text("Điền nhanh mã $generatedOtpHint") },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(6)
                                otpCode = digits
                                if (errorMessage != null) onClearError()
                            },
                            label = { Text("Mã OTP (6 chữ số)") },
                            placeholder = { Text("123456") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Pin,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onVerifyOtp(email, otpCode)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onVerifyOtp(email, otpCode)
                            },
                            enabled = !isLoading && otpCode.length == 6,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = "Xác minh mã OTP",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                onSendOtp(email) { resendCountdown = 60 }
                            },
                            enabled = !isLoading && resendCountdown == 0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = if (resendCountdown > 0) {
                                    "Gửi lại mã sau ${resendCountdown}s"
                                } else {
                                    "Gửi lại mã OTP mới"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    else -> {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                if (errorMessage != null) onClearError()
                            },
                            label = { Text("Mật khẩu mới *") },
                            placeholder = { Text("Tối thiểu 6 ký tự") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.LockReset,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPassword = !showPassword },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = if (showPassword) {
                                            "Ẩn mật khẩu"
                                        } else {
                                            "Hiện mật khẩu"
                                        }
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (errorMessage != null) onClearError()
                            },
                            label = { Text("Xác nhận mật khẩu mới *") },
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onResetPassword(email, otpCode, newPassword, confirmPassword)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onResetPassword(email, otpCode, newPassword, confirmPassword)
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = "Cập nhật mật khẩu",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
