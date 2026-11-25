package nhom3.chatbln.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nhom3.chatbln.android.data.model.CachedUser

enum class UserPresenceMode(val rawValue: String, val title: String) {
    ONLINE("online", "Trực tuyến"),
    AWAY("away", "Tạm vắng"),
    BUSY("busy", "Đang bận"),
    OFFLINE("offline", "Ẩn trạng thái (Ngoại tuyến)");

    companion object {
        fun fromRaw(raw: String?): UserPresenceMode {
            return entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) } ?: ONLINE
        }
    }
}

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chat_bln_session_prefs"
)

class SessionPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var currentToken: String? = null
        private set

    @Volatile
    var currentBaseUrl: String = DEFAULT_PRODUCTION_BASE_URL
        private set

    init {
        scope.launch {
            appContext.sessionDataStore.data.collectLatest { prefs ->
                currentToken = prefs[KEY_AUTH_TOKEN]?.takeIf { it.isNotBlank() }
                currentBaseUrl = normalizeServerUrl(prefs[KEY_SERVER_BASE_URL])
            }
        }
    }

    val tokenFlow: Flow<String?> = appContext.sessionDataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN]?.takeIf { it.isNotBlank() }
    }

    val cachedUserFlow: Flow<CachedUser?> = appContext.sessionDataStore.data.map { prefs ->
        val raw = prefs[KEY_CACHED_USER] ?: return@map null
        runCatching { gson.fromJson(raw, CachedUser::class.java) }.getOrNull()
    }

    val darkModeFlow: Flow<Boolean?> = appContext.sessionDataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE]
    }

    val presenceModeFlow: Flow<UserPresenceMode> = appContext.sessionDataStore.data.map { prefs ->
        UserPresenceMode.fromRaw(prefs[KEY_PRESENCE_MODE])
    }

    val serverBaseUrlFlow: Flow<String> = appContext.sessionDataStore.data.map { prefs ->
        normalizeServerUrl(prefs[KEY_SERVER_BASE_URL])
    }

    suspend fun awaitInitialSession(): Pair<String?, CachedUser?> {
        val prefs = appContext.sessionDataStore.data.first()
        val token = prefs[KEY_AUTH_TOKEN]?.takeIf { it.isNotBlank() }
        val baseUrl = normalizeServerUrl(prefs[KEY_SERVER_BASE_URL])
        currentToken = token
        currentBaseUrl = baseUrl
        val user = prefs[KEY_CACHED_USER]?.let { raw ->
            runCatching { gson.fromJson(raw, CachedUser::class.java) }.getOrNull()
        }
        return token to user
    }

    suspend fun saveAuthSession(token: String, user: CachedUser) {
        val cleanToken = token.trim()
        currentToken = cleanToken
        appContext.sessionDataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = cleanToken
            prefs[KEY_CACHED_USER] = gson.toJson(user)
        }
    }

    suspend fun updateCachedUser(user: CachedUser) {
        appContext.sessionDataStore.edit { prefs ->
            prefs[KEY_CACHED_USER] = gson.toJson(user)
        }
    }

    suspend fun clearAuthSession() {
        currentToken = null
        appContext.sessionDataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_CACHED_USER)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        appContext.sessionDataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun setPresenceMode(mode: UserPresenceMode) {
        appContext.sessionDataStore.edit { prefs ->
            prefs[KEY_PRESENCE_MODE] = mode.rawValue
        }
    }

    suspend fun setServerBaseUrl(rawUrl: String): String {
        val normalized = normalizeServerUrl(rawUrl)
        currentBaseUrl = normalized
        appContext.sessionDataStore.edit { prefs ->
            prefs[KEY_SERVER_BASE_URL] = normalized
        }
        return normalized
    }

    fun buildWebSocketUrl(baseUrl: String = currentBaseUrl): String {
        val clean = normalizeServerUrl(baseUrl)
        val wsBase = when {
            clean.startsWith("https://", ignoreCase = true) ->
                "wss://" + clean.removePrefix("https://").removePrefix("HTTPS://")
            clean.startsWith("http://", ignoreCase = true) ->
                "ws://" + clean.removePrefix("http://").removePrefix("HTTP://")
            else -> "ws://$clean"
        }
        return "${wsBase.trimEnd('/')}/ws-native"
    }

    companion object {
        const val DEFAULT_PRODUCTION_BASE_URL = "https://chat-bln.fishu.io.vn"
        const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8088"
        const val RETROFIT_PLACEHOLDER_API_URL = "http://10.0.2.2:8088/api/"

        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_CACHED_USER = stringPreferencesKey("cached_user_json")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_PRESENCE_MODE = stringPreferencesKey("presence_mode")
        private val KEY_SERVER_BASE_URL = stringPreferencesKey("server_base_url")

        fun normalizeServerUrl(input: String?): String {
            val trimmed = input?.trim().orEmpty()
            if (trimmed.isEmpty()) return DEFAULT_PRODUCTION_BASE_URL

            val withScheme = if (
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                val scheme = if (shouldDefaultToCleartextHttp(trimmed)) "http" else "https"
                "$scheme://$trimmed"
            }

            val cleaned = withScheme
                .trimEnd('/')
                .removeSuffix("/api")
                .removeSuffix("/ws-native")
                .trimEnd('/')
                .ifEmpty { DEFAULT_PRODUCTION_BASE_URL }

            return if (cleaned.equals("http://chat-bln.fishu.io.vn", ignoreCase = true)) {
                DEFAULT_PRODUCTION_BASE_URL
            } else {
                cleaned
            }
        }

        private fun shouldDefaultToCleartextHttp(rawHostPort: String): Boolean {
            val hostPort = rawHostPort.substringBefore('/').lowercase()
            if (hostPort.contains(":8088") || hostPort.contains(":8080") || hostPort.contains(":4200")) {
                return true
            }
            val hostOnly = hostPort.substringBefore(':')
            if (hostOnly == "localhost" || hostOnly.endsWith(".local")) {
                return true
            }
            val octets = hostOnly.split('.')
            if (octets.size == 4 && octets.all { it.toIntOrNull() != null }) {
                return true
            }
            return false
        }
    }
}
