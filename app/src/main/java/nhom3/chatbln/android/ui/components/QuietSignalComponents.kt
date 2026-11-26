package nhom3.chatbln.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.local.UserPresenceMode
import nhom3.chatbln.android.ui.theme.DndIndicatorDark
import nhom3.chatbln.android.ui.theme.DndIndicatorLight
import nhom3.chatbln.android.ui.theme.IdleIndicatorDark
import nhom3.chatbln.android.ui.theme.IdleIndicatorLight
import nhom3.chatbln.android.ui.theme.OnlineIndicatorDark
import nhom3.chatbln.android.ui.theme.OnlineIndicatorLight
import nhom3.chatbln.android.ui.theme.SleepIndicatorDark
import nhom3.chatbln.android.ui.theme.SleepIndicatorLight
import nhom3.chatbln.android.ui.theme.rememberAnimationsEnabled

data class PresenceVisualSpec(
    val icon: ImageVector,
    val color: Color,
    val label: String
)

@Composable
fun resolvePresenceVisual(
    mode: UserPresenceMode,
    isWsConnected: Boolean = true
): PresenceVisualSpec {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    if (mode == UserPresenceMode.OFFLINE) {
        return PresenceVisualSpec(
            icon = Icons.Outlined.RadioButtonUnchecked,
            color = MaterialTheme.colorScheme.outline,
            label = "Ẩn trạng thái"
        )
    }
    if (!isWsConnected) {
        return PresenceVisualSpec(
            icon = Icons.Outlined.WifiOff,
            color = if (isDark) IdleIndicatorDark else IdleIndicatorLight,
            label = "${mode.shortTitle} · Đang nối"
        )
    }
    return when (mode) {
        UserPresenceMode.ONLINE -> PresenceVisualSpec(
            icon = Icons.Filled.Circle,
            color = if (isDark) OnlineIndicatorDark else OnlineIndicatorLight,
            label = mode.shortTitle
        )
        UserPresenceMode.IDLE -> PresenceVisualSpec(
            icon = Icons.Outlined.Schedule,
            color = if (isDark) IdleIndicatorDark else IdleIndicatorLight,
            label = mode.shortTitle
        )
        UserPresenceMode.SLEEP -> PresenceVisualSpec(
            icon = Icons.Outlined.Bedtime,
            color = if (isDark) SleepIndicatorDark else SleepIndicatorLight,
            label = mode.shortTitle
        )
        UserPresenceMode.DND -> PresenceVisualSpec(
            icon = Icons.Outlined.DoNotDisturbOn,
            color = if (isDark) DndIndicatorDark else DndIndicatorLight,
            label = mode.shortTitle
        )
        UserPresenceMode.OFFLINE -> PresenceVisualSpec(
            icon = Icons.Outlined.RadioButtonUnchecked,
            color = MaterialTheme.colorScheme.outline,
            label = mode.shortTitle
        )
    }
}

@Composable
fun resolvePeerPresenceIcon(
    isOnline: Boolean?,
    presenceStatus: String?
): Pair<ImageVector, Color> {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    if (isOnline != true) {
        return Icons.Outlined.RadioButtonUnchecked to MaterialTheme.colorScheme.outline
    }
    return when (UserPresenceMode.fromRaw(presenceStatus)) {
        UserPresenceMode.IDLE ->
            Icons.Outlined.Schedule to (if (isDark) IdleIndicatorDark else IdleIndicatorLight)
        UserPresenceMode.SLEEP ->
            Icons.Outlined.Bedtime to (if (isDark) SleepIndicatorDark else SleepIndicatorLight)
        UserPresenceMode.DND ->
            Icons.Outlined.DoNotDisturbOn to (if (isDark) DndIndicatorDark else DndIndicatorLight)
        else ->
            Icons.Filled.Circle to (if (isDark) OnlineIndicatorDark else OnlineIndicatorLight)
    }
}

/**
 * Unified Chat BLN "Signal Aperture" Brand Mark — pixel-consistent with Web `logo.svg`,
 * Android `ic_launcher_foreground.xml`, and iOS `ChatBLNBrandMark`.
 */
@Composable
fun QuietSignalBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val cornerRadius = (size.value * 0.25f).dp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1758EE), Color(0xFF0A369D))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.50f
            val cy = h * 0.515f
            val r = w * 0.275f
            val strokeW = w * 0.072f

            // Open geometric chat aperture arc with bottom-left speech tail
            val arcRect = androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r)
            val path = Path().apply {
                arcTo(arcRect, startAngleDegrees = -12f, sweepAngleDegrees = 130f, forceMoveTo = true)
                lineTo(w * 0.23f, h * 0.77f)
                val upperRad = Math.toRadians(148.0)
                lineTo(cx + r * cos(upperRad).toFloat(), cy + r * sin(upperRad).toFloat())
                arcTo(arcRect, startAngleDegrees = 148f, sweepAngleDegrees = 148f, forceMoveTo = false)
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Ascending precision signal bars
            val barW = w * 0.074f
            val barRadius = CornerRadius(barW * 0.5f, barW * 0.5f)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.375f, h * 0.46f),
                size = Size(barW, h * 0.145f),
                cornerRadius = barRadius
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.495f, h * 0.385f),
                size = Size(barW, h * 0.22f),
                cornerRadius = barRadius
            )

            // Live sky-cyan realtime beacon dot
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = w * 0.065f,
                center = Offset(w * 0.695f, h * 0.315f)
            )
        }
    }
}

enum class AlertBannerTone {
    ERROR,
    SUCCESS,
    INFO
}

@Composable
fun InlineAlertBanner(
    message: String,
    tone: AlertBannerTone = AlertBannerTone.ERROR,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, borderColor, icon) = when (tone) {
        AlertBannerTone.ERROR -> Quadruple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
            Icons.Outlined.ErrorOutline
        )
        AlertBannerTone.SUCCESS -> Quadruple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
            Icons.Outlined.CheckCircleOutline
        )
        AlertBannerTone.INFO -> Quadruple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.outlineVariant,
            Icons.Outlined.Info
        )
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun rememberSkeletonAlpha(): Float {
    val animationsEnabled = rememberAnimationsEnabled()
    if (!animationsEnabled) return 0.72f
    val transition = rememberInfiniteTransition(label = "quiet_signal_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    return alpha
}

@Composable
fun TonalListSkeleton(
    rowCount: Int = 6,
    showTrailingAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pulseAlpha = rememberSkeletonAlpha()
    val blockColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = pulseAlpha)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Đang tải danh sách" }
    ) {
        repeat(rowCount) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(blockColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.52f else 0.40f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(blockColor)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.80f else 0.65f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(blockColor.copy(alpha = pulseAlpha * 0.75f))
                    )
                }
                if (showTrailingAction) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .width(68.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(blockColor)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageHistorySkeleton(
    modifier: Modifier = Modifier
) {
    val pulseAlpha = rememberSkeletonAlpha()
    val incomingColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = pulseAlpha)
    val outgoingColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha * 0.85f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .semantics { contentDescription = "Đang tải tin nhắn" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) { idx ->
            val isMine = idx % 2 == 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                if (!isMine) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(incomingColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (idx % 3 == 0) 0.64f else 0.48f)
                        .height(if (idx == 2) 64.dp else 44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isMine) outgoingColor else incomingColor)
                )
            }
        }
    }
}
