package nhom3.chatbln.android.ui.components

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import nhom3.chatbln.android.data.local.UserPresenceMode
import nhom3.chatbln.android.ui.theme.DndIndicatorDark
import nhom3.chatbln.android.ui.theme.DndIndicatorLight
import nhom3.chatbln.android.ui.theme.IdleIndicatorDark
import nhom3.chatbln.android.ui.theme.IdleIndicatorLight
import nhom3.chatbln.android.ui.theme.OnlineIndicatorDark
import nhom3.chatbln.android.ui.theme.OnlineIndicatorLight
import nhom3.chatbln.android.ui.theme.SleepIndicatorDark
import nhom3.chatbln.android.ui.theme.SleepIndicatorLight
import java.util.Locale

@Composable
fun AvatarView(
    displayName: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    presenceStatus: String? = null,
    showOnlineBadge: Boolean = false,
    isGroup: Boolean = false
) {
    val context = LocalContext.current
    val initials = remember(displayName) { computeInitials(displayName) }
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mode = remember(presenceStatus) { UserPresenceMode.fromRaw(presenceStatus) }

    val badgeColor = when (mode) {
        UserPresenceMode.IDLE -> if (darkTheme) IdleIndicatorDark else IdleIndicatorLight
        UserPresenceMode.SLEEP -> if (darkTheme) SleepIndicatorDark else SleepIndicatorLight
        UserPresenceMode.DND -> if (darkTheme) DndIndicatorDark else DndIndicatorLight
        else -> if (darkTheme) OnlineIndicatorDark else OnlineIndicatorLight
    }

    val statusDescription = if (showOnlineBadge && !isGroup) {
        if (isOnline) "$displayName, ${mode.shortTitle}" else "$displayName, Ngoại tuyến"
    } else {
        displayName
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = statusDescription }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (isGroup) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                val imageRequest = remember(context, avatarUrl) {
                    val dataModel: Any = if (
                        avatarUrl.startsWith("data:", ignoreCase = true) &&
                        avatarUrl.contains("base64,")
                    ) {
                        runCatching {
                            Base64.decode(
                                avatarUrl.substringAfter("base64,"),
                                Base64.DEFAULT
                            )
                        }.getOrNull() ?: avatarUrl
                    } else {
                        avatarUrl
                    }
                    val cacheKey = "avatar_${avatarUrl.hashCode()}"
                    ImageRequest.Builder(context)
                        .data(dataModel)
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(false)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        InitialsFallback(
                            initials = initials,
                            size = size,
                            isGroup = isGroup
                        )
                    },
                    error = {
                        InitialsFallback(
                            initials = initials,
                            size = size,
                            isGroup = isGroup
                        )
                    }
                )
            } else {
                InitialsFallback(
                    initials = initials,
                    size = size,
                    isGroup = isGroup
                )
            }
        }

        if (showOnlineBadge && isOnline && !isGroup) {
            val badgeSize = (size.value * 0.30f).coerceIn(12f, 18f).dp
            val glyphIcon = when (mode) {
                UserPresenceMode.IDLE -> Icons.Outlined.Schedule
                UserPresenceMode.SLEEP -> Icons.Outlined.Bedtime
                UserPresenceMode.DND -> Icons.Outlined.Remove
                else -> null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (glyphIcon != null && badgeSize >= 14.dp) {
                    Icon(
                        imageVector = glyphIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size((badgeSize.value * 0.62f).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialsFallback(
    initials: String,
    size: Dp,
    isGroup: Boolean
) {
    if (isGroup) {
        Icon(
            imageVector = Icons.Outlined.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size((size.value * 0.50f).dp)
        )
    } else {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.35f).coerceIn(12f, 26f).sp
        )
    }
}

private fun computeInitials(name: String): String {
    val parts = name.trim()
        .removePrefix("@")
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) {
        return parts[0].take(2).uppercase(Locale.getDefault())
    }
    val first = parts.first().first()
    val last = parts.last().first()
    return "$first$last".uppercase(Locale.getDefault())
}
