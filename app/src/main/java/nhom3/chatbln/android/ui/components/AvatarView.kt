package nhom3.chatbln.android.ui.components

import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
                        DefaultAvatarArtwork(
                            isGroup = isGroup,
                            darkTheme = darkTheme
                        )
                    },
                    error = {
                        DefaultAvatarArtwork(
                            isGroup = isGroup,
                            darkTheme = darkTheme
                        )
                    }
                )
            } else {
                DefaultAvatarArtwork(
                    isGroup = isGroup,
                    darkTheme = darkTheme
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
private fun DefaultAvatarArtwork(
    isGroup: Boolean,
    darkTheme: Boolean
) {
    val bgColor = if (darkTheme) {
        Color(0xFF172554)
    } else if (isGroup) {
        Color(0xFFDBEAFE)
    } else {
        Color(0xFFE6EFFC)
    }
    val primaryColor = if (darkTheme) Color(0xFFF8FAFC) else Color(0xFF0F52BA)
    val accentColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val s = minOf(size.width, size.height) / 128f
        val ox = (size.width - 128f * s) * 0.5f
        val oy = (size.height - 128f * s) * 0.5f
        fun px(x: Float) = ox + x * s
        fun py(y: Float) = oy + y * s
        fun rrectPath(
            x0: Float, y0: Float, x1: Float, y1: Float,
            rtl: Float, rtr: Float, rbr: Float, rbl: Float
        ): Path {
            val l = px(x0); val t = py(y0); val r = px(x1); val b = py(y1)
            val tl = rtl * s; val tr = rtr * s; val br = rbr * s; val bl = rbl * s
            return Path().apply {
                moveTo(l + tl, t)
                arcTo(Rect(r - 2 * tr, t, r, t + 2 * tr), 270f, 90f, false)
                arcTo(Rect(r - 2 * br, b - 2 * br, r, b), 0f, 90f, false)
                arcTo(Rect(l, b - 2 * bl, l + 2 * bl, b), 90f, 90f, false)
                arcTo(Rect(l, t, l + 2 * tl, t + 2 * tl), 180f, 90f, false)
                close()
            }
        }

        drawRect(color = bgColor)

        if (isGroup) {
            // Back-right secondary member separated by exact 5px concentric negative-space moat
            drawCircle(
                color = accentColor,
                radius = 12f * s,
                center = Offset(px(84f), py(49f))
            )
            val backTorso = Path().apply {
                moveTo(px(71.52f), py(70f))
                lineTo(px(90f), py(70f))
                arcTo(Rect(px(73f), py(70f), px(107f), py(104f)), 270f, 90f, false)
                arcTo(Rect(px(89f), py(78f), px(107f), py(96f)), 0f, 90f, false)
                lineTo(px(75.71f), py(96f))
                arcTo(Rect(px(32f), py(63f), px(78f), py(109f)), 25.77f, -69.85f, false)
                close()
            }
            drawPath(path = backTorso, color = accentColor)

            // Front-left primary member
            drawCircle(
                color = primaryColor,
                radius = 15f * s,
                center = Offset(px(47f), py(45f))
            )
            drawPath(
                path = rrectPath(21f, 68f, 73f, 96f, 18f, 18f, 10f, 10f),
                color = primaryColor
            )
        } else {
            // Unified B-Dialogue geometric default user silhouette
            drawPath(
                path = rrectPath(29f, 72f, 99f, 102f, 20f, 20f, 10f, 10f),
                color = accentColor
            )
            drawCircle(
                color = primaryColor,
                radius = 17f * s,
                center = Offset(px(64f), py(45f))
            )
        }
    }
}
