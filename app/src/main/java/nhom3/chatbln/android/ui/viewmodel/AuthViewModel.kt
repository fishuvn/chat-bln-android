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
    val serverBaseUrl: String = SessionPreferences.DEFAULT_EMULATOR_BASE_URL,
    val darkModePreference: Boolean? = null
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
                it.copy(errorMessage = "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.login(username = cleanUser, password = password)
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

    fun updateServerUrl(newUrl: String) {
        viewModelScope.launch {
            val saved = sessionPreferences.setServerBaseUrl(newUrl)
            container.stompRealtimeClient.reconnectWithLatestEndpoint()
            _uiState.update { it.copy(serverBaseUrl = saved, errorMessage = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
