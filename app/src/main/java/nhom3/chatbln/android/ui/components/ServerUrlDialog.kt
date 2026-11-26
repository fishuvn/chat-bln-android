package nhom3.chatbln.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nhom3.chatbln.android.data.local.SessionPreferences

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServerUrlDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var draftUrl by rememberSaveable(currentUrl) { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Địa chỉ máy chủ",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Mặc định kết nối HTTPS tới chat-bln.fishu.io.vn. Khi kiểm thử cục bộ dùng 10.0.2.2:8088 (máy ảo) hoặc địa chỉ IP LAN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = draftUrl,
                    onValueChange = { draftUrl = it },
                    label = { Text("Server Base URL") },
                    placeholder = { Text("https://chat-bln.fishu.io.vn") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { draftUrl = SessionPreferences.DEFAULT_PRODUCTION_BASE_URL },
                        label = { Text("Cloudflare HTTPS") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                    AssistChip(
                        onClick = { draftUrl = SessionPreferences.DEFAULT_EMULATOR_BASE_URL },
                        label = { Text("Máy ảo 10.0.2.2") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                    AssistChip(
                        onClick = { draftUrl = "http://192.168.1.10:8088" },
                        label = { Text("Mạng LAN") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(SessionPreferences.normalizeServerUrl(draftUrl))
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Lưu cấu hình")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Hủy")
            }
        }
    )
}
