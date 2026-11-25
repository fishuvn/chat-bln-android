package nhom3.chatbln.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import nhom3.chatbln.android.ui.theme.OnlineIndicatorDark
import nhom3.chatbln.android.ui.theme.OnlineIndicatorLight
import java.util.Locale

@Composable
fun AvatarView(
    displayName: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    showOnlineBadge: Boolean = false,
    isGroup: Boolean = false
) {
    val initials = computeInitials(displayName)
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val onlineDotColor = if (darkTheme) OnlineIndicatorDark else OnlineIndicatorLight

    Box(modifier = modifier.size(size)) {
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
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = avatarUrl,
                    contentDescription = displayName,
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
            val dotSize = (size.value * 0.28f).coerceIn(11f, 16f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(onlineDotColor)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
            )
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
            modifier = Modifier.size((size.value * 0.52f).dp)
        )
    } else {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.36f).coerceIn(12f, 28f).sp
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
