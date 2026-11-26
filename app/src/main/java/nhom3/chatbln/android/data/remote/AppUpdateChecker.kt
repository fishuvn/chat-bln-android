package nhom3.chatbln.android.data.remote

import android.os.Build
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nhom3.chatbln.android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GitHubReleaseAssetDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("browser_download_url") val downloadUrl: String = "",
    @SerializedName("size") val size: Long = 0L
)

data class GitHubReleaseDto(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("html_url") val htmlUrl: String = "",
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("assets") val assets: List<GitHubReleaseAssetDto> = emptyList()
)

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val latestTag: String,
    val releaseTitle: String,
    val changelog: String,
    val publishedDate: String,
    val releasePageUrl: String,
    val recommendedAsset: GitHubReleaseAssetDto?,
    val checksumsUrl: String?,
    val hasUpdate: Boolean
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class UpToDate(val info: AppUpdateInfo) : UpdateCheckState
    data class Available(val info: AppUpdateInfo) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

object AppUpdateChecker {
    const val GITHUB_REPO = "fishuvn/chat-bln-android"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    fun primaryAbiLabel(): String {
        val abis = Build.SUPPORTED_ABIS ?: emptyArray()
        return when {
            abis.any { it.equals("arm64-v8a", ignoreCase = true) } -> "arm64-v8a"
            abis.any { it.equals("armeabi-v7a", ignoreCase = true) } -> "armeabi-v7a"
            else -> "universal"
        }
    }

    fun isNewerVersion(currentRaw: String, latestRaw: String): Boolean {
        val cur = parseSemVer(currentRaw)
        val lat = parseSemVer(latestRaw)
        if (cur != null && lat != null) {
            if (lat.first != cur.first) return lat.first > cur.first
            if (lat.second != cur.second) return lat.second > cur.second
            return lat.third > cur.third
        }
        val cleanCur = currentRaw.trim().trimStart('v', 'V')
        val cleanLat = latestRaw.trim().trimStart('v', 'V')
        return cleanLat.isNotEmpty() && cleanLat != cleanCur
    }

    private fun parseSemVer(raw: String): Triple<Int, Int, Int>? {
        val clean = raw.trim().trimStart('v', 'V').substringBefore('-')
        val parts = clean.split('.')
        if (parts.size < 2) return null
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return Triple(major, minor, patch)
    }

    fun selectBestApkAsset(
        assets: List<GitHubReleaseAssetDto>,
        abi: String = primaryAbiLabel()
    ): GitHubReleaseAssetDto? {
        val preferred = when (abi) {
            "arm64-v8a" -> "arm64-v8a.apk"
            "armeabi-v7a" -> "arm32-v7a.apk"
            else -> "universal.apk"
        }
        return assets.firstOrNull { it.name.lowercase().contains(preferred) }
            ?: assets.firstOrNull { it.name.lowercase().contains("universal.apk") }
            ?: assets.firstOrNull { it.name.lowercase().endsWith(".apk") }
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576L -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024L -> String.format("%.1f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }

    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val currentVersion = BuildConfig.VERSION_NAME
            val request = Request.Builder()
                .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ChatBLN-Android/$currentVersion")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                when (response.code) {
                    404 -> error("Chưa có bản phát hành nào trên GitHub.")
                    403 -> error("GitHub API tạm giới hạn lượt kiểm tra. Thử lại sau vài phút.")
                }
                if (!response.isSuccessful) {
                    error("Không thể kiểm tra cập nhật (HTTP ${response.code}).")
                }
                val bodyStr = response.body?.string().orEmpty()
                val dto = gson.fromJson(bodyStr, GitHubReleaseDto::class.java)
                    ?: error("Dữ liệu bản phát hành không hợp lệ.")

                val latestClean = dto.tagName.trim().trimStart('v', 'V')
                val hasUpdate = isNewerVersion(currentVersion, latestClean)
                val recommended = selectBestApkAsset(dto.assets)
                val checksums = dto.assets
                    .firstOrNull { it.name.equals("SHA256SUMS.txt", ignoreCase = true) }
                    ?.downloadUrl

                AppUpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = latestClean,
                    latestTag = dto.tagName.ifBlank { "v$latestClean" },
                    releaseTitle = dto.name?.takeIf { it.isNotBlank() } ?: "Chat BLN ${dto.tagName}",
                    changelog = dto.body?.takeIf { it.isNotBlank() }
                        ?: "Bản phát hành mới kèm tối ưu hiệu năng và sửa lỗi.",
                    publishedDate = dto.publishedAt?.substringBefore('T').orEmpty(),
                    releasePageUrl = dto.htmlUrl,
                    recommendedAsset = recommended,
                    checksumsUrl = checksums,
                    hasUpdate = hasUpdate
                )
            }
        }
    }
}
