package nhom3.chatbln.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nhom3.chatbln.android.AppContainer
import nhom3.chatbln.android.data.local.SessionPreferences
import nhom3.chatbln.android.data.model.RegisterRequest

data class AuthUiState(
    val isBootstrapping: Boolean = true,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccessNotice: String? = null,
    val forgotStep: Int = 1,
    val forgotEmail: String = "",
    val forgotInfoMessage: String? = null,
    val forgotGeneratedOtp: String? = null,
    val serverBaseUrl: String = SessionPreferences.DEFAULT_PRODUCTION_BASE_URL,
    val darkModePreference: Boolean? = null,
    val dynamicColorEnabled: Boolean = false
)

class AuthViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val sessionPreferences = container.sessionPreferences
    private val repository = container.chatRepository

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val (token, _) = sessionPreferences.awaitInitialSession()
            _uiState.update {
                it.copy(
                    isBootstrapping = false,
                    isAuthenticated = !token.isNullOrBlank(),
                    serverBaseUrl = sessionPreferences.currentBaseUrl
                )
            }
        }

        viewModelScope.launch {
            sessionPreferences.tokenFlow.collectLatest { token ->
                _uiState.update { state ->
                    if (state.isBootstrapping) {
                        state
                    } else {
                        state.copy(isAuthenticated = !token.isNullOrBlank())
                    }
                }
            }
        }

        viewModelScope.launch {
            sessionPreferences.serverBaseUrlFlow.collectLatest { baseUrl ->
                _uiState.update { it.copy(serverBaseUrl = baseUrl) }
            }
        }

        viewModelScope.launch {
            sessionPreferences.darkModeFlow.collectLatest { darkPref ->
                _uiState.update { it.copy(darkModePreference = darkPref) }
            }
        }

        viewModelScope.launch {
            sessionPreferences.dynamicColorFlow.collectLatest { dynamicEnabled ->
                _uiState.update { it.copy(dynamicColorEnabled = dynamicEnabled) }
            }
        }

        viewModelScope.launch {
            container.authInterceptor.unauthorizedEvents.collectLatest {
                container.stompRealtimeClient.disconnect()
                sessionPreferences.clearAuthSession()
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        errorMessage = "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."
                    )
                }
            }
        }
    }

    fun login(username: String, password: String) {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty() || password.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.",
                    loginSuccessNotice = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loginSuccessNotice = null
                )
            }
            val result = repository.login(username = cleanUser, password = password)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            errorMessage = null,
                            loginSuccessNotice = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Đăng nhập thất bại."
                        )
                    }
                }
            )
        }
    }

    fun register(
        username: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        email: String,
        phone: String,
        gender: String,
        dob: String,
        bio: String
    ) {
        val cleanUser = username.trim()
        val cleanName = fullName.trim()
        val cleanDob = dob.trim()

        when {
            cleanName.isEmpty() -> {
                _uiState.update { it.copy(errorMessage = "Vui lòng nhập họ và tên hiển thị.") }
                return
            }
            cleanUser.length < 3 -> {
                _uiState.update { it.copy(errorMessage = "Tên đăng nhập cần từ 3 ký tự trở lên.") }
                return
            }
            password.length < 6 -> {
                _uiState.update { it.copy(errorMessage = "Mật khẩu cần từ 6 ký tự trở lên.") }
                return
            }
            password != confirmPassword -> {
                _uiState.update { it.copy(errorMessage = "Mật khẩu xác nhận không trùng khớp.") }
                return
            }
            cleanDob.isNotEmpty() && !Regex("""^\d{4}-\d{2}-\d{2}$""").matches(cleanDob) -> {
                _uiState.update { it.copy(errorMessage = "Ngày sinh cần theo định dạng YYYY-MM-DD.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val request = RegisterRequest(
                username = cleanUser,
                password = password,
                fullName = cleanName,
                email = email.trim().ifEmpty { null },
                phone = phone.trim().ifEmpty { null },
                gender = gender.trim().ifEmpty { null },
                dob = cleanDob.ifEmpty { null },
                bio = bio.trim().ifEmpty { null }
            )
            val result = repository.register(request)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Đăng ký tài khoản thất bại."
                        )
                    }
                }
            )
        }
    }

    fun requestForgotPasswordOtp(
        email: String,
        onOtpIssued: () -> Unit = {}
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập địa chỉ email đã đăng ký.") }
            return
        }
        if (!Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(cleanEmail)) {
            _uiState.update { it.copy(errorMessage = "Địa chỉ email không đúng định dạng.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    forgotInfoMessage = null
                )
            }
            repository.requestPasswordResetOtp(cleanEmail).fold(
                onSuccess = { resp ->
                    val targetEmail = resp.email?.takeIf { it.isNotBlank() } ?: cleanEmail
                    val issuedOtp = resp.otp?.takeIf { it.isNotBlank() }
                    val info = when {
                        !issuedOtp.isNullOrBlank() ->
                            "Mã xác thực 6 chữ số cho $targetEmail: $issuedOtp (hiệu lực 5 phút)."
                        !resp.message.isNullOrBlank() -> resp.message
                        else -> "Đã gửi mã xác thực 6 chữ số tới $targetEmail."
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            forgotStep = 2,
                            forgotEmail = targetEmail,
                            forgotInfoMessage = info,
                            forgotGeneratedOtp = issuedOtp
                        )
                    }
                    onOtpIssued()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Không tìm thấy tài khoản gắn với email này."
                        )
                    }
                }
            )
        }
    }

    fun verifyForgotPasswordOtp(email: String, otp: String) {
        val cleanEmail = email.trim().ifEmpty { _uiState.value.forgotEmail }
        val cleanOtp = otp.trim()
        if (cleanOtp.length != 6 || !cleanOtp.all { it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập đúng mã OTP gồm 6 chữ số.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.verifyPasswordResetOtp(email = cleanEmail, otp = cleanOtp).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            forgotStep = 3,
                            forgotEmail = cleanEmail,
                            forgotInfoMessage = "Đã xác minh mã OTP. Vui lòng đặt mật khẩu mới."
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Mã OTP không chính xác hoặc đã hết hạn."
                        )
                    }
                }
            )
        }
    }

    fun resetPasswordWithOtp(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String,
        onResetSuccess: () -> Unit
    ) {
        val cleanEmail = email.trim().ifEmpty { _uiState.value.forgotEmail }
        val cleanOtp = otp.trim()
        when {
            newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                _uiState.update {
                    it.copy(errorMessage = "Vui lòng nhập đầy đủ mật khẩu mới và xác nhận.")
                }
                return
            }
            newPassword.length < 6 -> {
                _uiState.update {
                    it.copy(errorMessage = "Mật khẩu mới cần tối thiểu 6 ký tự.")
                }
                return
            }
            newPassword != confirmPassword -> {
                _uiState.update {
                    it.copy(errorMessage = "Mật khẩu xác nhận không trùng khớp.")
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.resetPasswordWithOtp(
                email = cleanEmail,
                otp = cleanOtp,
                newPassword = newPassword
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            forgotStep = 1,
                            forgotInfoMessage = null,
                            forgotGeneratedOtp = null,
                            loginSuccessNotice = "Đã đặt lại mật khẩu mới thành công. Bạn có thể đăng nhập ngay."
                        )
                    }
                    onResetSuccess()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Không thể cập nhật mật khẩu mới."
                        )
                    }
                }
            )
        }
    }

    fun stepBackForgotFlow(): Boolean {
        val step = _uiState.value.forgotStep
        return if (step > 1) {
            _uiState.update {
                it.copy(
                    forgotStep = step - 1,
                    errorMessage = null
                )
            }
            true
        } else {
            false
        }
    }

    fun resetForgotFlow() {
        _uiState.update {
            it.copy(
                forgotStep = 1,
                forgotInfoMessage = null,
                forgotGeneratedOtp = null,
                errorMessage = null
            )
        }
    }

    fun updateServerUrl(newUrl: String) {
        viewModelScope.launch {
            val saved = sessionPreferences.setServerBaseUrl(newUrl)
            container.stompRealtimeClient.reconnectWithLatestEndpoint()
            _uiState.update { it.copy(serverBaseUrl = saved, errorMessage = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, loginSuccessNotice = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(container) as T
                }
            }
    }
}
