package nhom3.chatbln.android.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ExtractedLink(
    val url: String,
    val label: String,
    val host: String
)

private val CombinedLinkRegex = Regex(
    """\[([^\]]+)]\((https?://[^\s)]+)\)|(https?://[^\s<>"']+)""",
    RegexOption.IGNORE_CASE
)

private val MarkdownPairRegex = Regex(
    """^\[([^\]]+)]\((https?://[^\s)]+)\)$""",
    RegexOption.IGNORE_CASE
)

fun buildMarkdownAnnotatedString(
    rawText: String,
    linkColor: Color
): AnnotatedString = buildAnnotatedString {
    if (rawText.isEmpty()) return@buildAnnotatedString

    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline
        )
    )

    var cursor = 0
    for (match in CombinedLinkRegex.findAll(rawText)) {
        if (match.range.first > cursor) {
            append(rawText.substring(cursor, match.range.first))
        }

        val token = match.value
        val mdMatch = MarkdownPairRegex.matchEntire(token)
        if (mdMatch != null) {
            val label = mdMatch.groupValues[1].ifBlank { mdMatch.groupValues[2] }
            val url = mdMatch.groupValues[2]
            withLink(LinkAnnotation.Url(url = url, styles = linkStyles)) {
                append(label)
            }
        } else {
            val cleanUrl = token.trimEnd('.', ',', ';', ')')
            val trailingPunctuation = token.substring(cleanUrl.length)
            withLink(LinkAnnotation.Url(url = cleanUrl, styles = linkStyles)) {
                append(cleanUrl)
            }
            if (trailingPunctuation.isNotEmpty()) {
                append(trailingPunctuation)
            }
        }
        cursor = match.range.last + 1
    }

    if (cursor < rawText.length) {
        append(rawText.substring(cursor))
    }
}

@Composable
fun MarkdownLinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary
) {
    val annotated = remember(text, linkColor) {
        buildMarkdownAnnotatedString(rawText = text, linkColor = linkColor)
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = textColor)
    )
}

fun extractFirstLink(text: String?): ExtractedLink? {
    if (text.isNullOrBlank()) return null
    val match = CombinedLinkRegex.find(text) ?: return null
    val mdMatch = MarkdownPairRegex.matchEntire(match.value)
    val url = if (mdMatch != null) {
        mdMatch.groupValues[2]
    } else {
        match.value.trimEnd('.', ',', ';', ')')
    }
    val host = extractHostName(url)
    val label = mdMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() } ?: host
    return ExtractedLink(url = url, label = label, host = host)
}

fun extractHostName(url: String): String {
    return runCatching {
        URI(url).host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: url
}

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val ShortDateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault())
private val FullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

fun parseBackendDateTime(iso: String?): LocalDateTime? {
    val raw = iso?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching { LocalDateTime.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
        ?: runCatching {
            val trimmedFraction = raw.substringBefore('.')
            LocalDateTime.parse(trimmedFraction)
        }.getOrNull()
}

fun formatMessageTime(iso: String?): String {
    val dt = parseBackendDateTime(iso) ?: return ""
    val today = LocalDate.now()
    return if (dt.toLocalDate() == today) {
        dt.format(TimeFormatter)
    } else {
        "${dt.format(TimeFormatter)} ${dt.format(ShortDateFormatter)}"
    }
}

fun formatConversationListTime(iso: String?): String {
    val dt = parseBackendDateTime(iso) ?: return ""
    val now = LocalDate.now()
    val date = dt.toLocalDate()
    return when {
        date == now -> dt.format(TimeFormatter)
        date.year == now.year -> dt.format(ShortDateFormatter)
        else -> dt.format(FullDateFormatter)
    }
}

fun formatPresenceLabel(
    isOnline: Boolean?,
    lastSeen: String?,
    presenceStatus: String? = null
): String {
    if (isOnline == true) {
        return when (presenceStatus?.trim()?.uppercase()) {
            "IDLE", "AWAY" -> "Tạm vắng (Idle)"
            "SLEEP" -> "Chế độ ngủ (Sleep)"
            "DND", "BUSY" -> "Không làm phiền (DND)"
            else -> "Trực tuyến"
        }
    }
    val dt = parseBackendDateTime(lastSeen) ?: return "Ngoại tuyến"
    val minutes = Duration.between(dt, LocalDateTime.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "Hoạt động vừa xong"
        minutes < 60 -> "Hoạt động $minutes phút trước"
        minutes < 24 * 60 -> "Hoạt động ${minutes / 60} giờ trước"
        else -> "Hoạt động ${minutes / (24 * 60)} ngày trước"
    }
}

fun formatVietnameseDate(isoDate: String?): String {
    val raw = isoDate?.trim()?.takeIf { it.isNotEmpty() } ?: return "Chưa cập nhật"
    return runCatching {
        LocalDate.parse(raw).format(FullDateFormatter)
    }.getOrDefault(raw)
}
