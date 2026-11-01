package dev.rooni.zefir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.ui.MessageBanner
import dev.rooni.zefir.ui.ZefirBadge

/** Добавление подписки (ui/screens/Add.kt): в точности как в оригинале. */
@Composable
fun AddProfileScreen(controller: AppController, onBack: () -> Unit, onDone: () -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SubScreen(title = controller.s("add_title"), onBack = onBack) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(28.dp))
                ZefirBadge(
                    size = 116.dp,
                    icon = Icons.Rounded.Link,
                    iconTint = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    controller.s("add_heading"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    controller.s("add_hint"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(controller.s("add_field")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    minLines = 1,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
                val error = controller.error
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    MessageBanner(error)
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
            Button(
                onClick = {
                    val pasted = controller.platform.clipboardText()?.trim().orEmpty()
                    if (text.isBlank() && pasted.isEmpty()) {
                        controller.error = controller.s("add_clipboard_empty")
                    } else {
                        text = if (text.isBlank()) pasted else text
                        controller.addFromText(text) { ok -> if (ok) onDone() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (controller.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.height(20.dp))
                    Spacer(Modifier.height(0.dp))
                    Text("  " + controller.s("add_paste"), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (text.isNotBlank()) {
                Button(
                    onClick = { controller.addFromText(text) { ok -> if (ok) onDone() } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(controller.s("add_action"), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick = {
                    controller.error = controller.s("add_manual")
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Rounded.FileOpen, null, modifier = Modifier.height(18.dp))
                Text("  " + controller.s("add_file"), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** Экран «Изменить» — имя подписки и ссылка (ui/screens/ProfileEdit.kt). */
@Composable
fun ProfileEditScreen(controller: AppController, onBack: () -> Unit) {
    val profile = controller.activeProfile
    var name by remember { mutableStateOf(profile?.name.orEmpty()) }

    SubScreen(title = controller.s("edit_title"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(controller.s("edit_name")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = profile?.url.orEmpty(),
                onValueChange = {},
                label = { Text(controller.s("edit_url")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (profile != null && name.isNotBlank()) controller.renameProfile(profile, name)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(controller.s("save"))
            }
        }
    }
}