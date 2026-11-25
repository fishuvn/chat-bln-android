package nhom3.chatbln.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class EmojiCategory(
    val title: String,
    val emojis: List<String>
)

private val EmojiCategories = listOf(
    EmojiCategory(
        title = "Cảm xúc",
        emojis = listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
            "😊", "😇", "🙂", "😉", "😌", "😍", "🥰", "😘",
            "😋", "😛", "😜", "🤪", "🤨", "🧐", "🤓", "😎",
            "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁",
            "😮", "😯", "😲", "😳", "🥺", "😢", "😭", "😤"
        )
    ),
    EmojiCategory(
        title = "Cử chỉ",
        emojis = listOf(
            "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙",
            "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💪",
            "👋", "🤚", "🖐️", "✋", "🖖", "👈", "👉", "👆"
        )
    ),
    EmojiCategory(
        title = "Trái tim",
        emojis = listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
            "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
            "💘", "💝", "💟", "✨", "🔥", "💯", "🎉", "🎊"
        )
    ),
    EmojiCategory(
        title = "Động vật",
        emojis = listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
            "🐧", "🐦", "🐤", "🦆", "🦉", "🦋", "🐢", "🐳"
        )
    ),
    EmojiCategory(
        title = "Ẩm thực",
        emojis = listOf(
            "🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍑",
            "🍔", "🍟", "🍕", "🌭", "🥪", "🍜", "🍣", "🍱",
            "🍰", "🎂", "🍩", "🍪", "☕", "🧋", "🍵", "🥤"
        )
    ),
    EmojiCategory(
        title = "Biểu tượng",
        emojis = listOf(
            "✅", "❌", "⭐", "🌟", "⚡", "💡", "📌", "📎",
            "📁", "💻", "📱", "⌚", "📷", "🎬", "🎵", "🚀",
            "🏆", "🎯", "🔔", "🔒", "🔑", "🕒", "💬", "📢"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeCategory = EmojiCategories[selectedCategoryIndex]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Biểu tượng cảm xúc",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                EmojiCategories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = { Text(category.title) }
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 46.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                items(activeCategory.emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onEmojiSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}
