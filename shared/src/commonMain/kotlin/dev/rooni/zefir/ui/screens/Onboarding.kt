package dev.rooni.zefir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import dev.rooni.zefir.ui.ActionButton
import dev.rooni.zefir.ui.AppTextField
import dev.rooni.zefir.ui.MessageBanner
import dev.rooni.zefir.ui.SubScreen

/** Онбординг: 4 шага из оригинала (welcome → подписка → сервисы → готово). */
@Composable
fun OnboardingScreen(controller: AppController, onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .size(width = if (index == step) 22.dp else 8.dp, height = 8.dp)
                        .background(
                            if (index <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                            CircleShape,
                        )
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        when (step) {
            0 -> Step(
                icon = Icons.Rounded.AutoAwesome,
                title = controller.s("ob_welcome_title"),
                text = controller.s("ob_welcome_text"),
            )
            1 -> Step(
                icon = Icons.Rounded.Cloud,
                title = controller.s("ob_add_title"),
                text = controller.s("ob_add_text"),
            )
            2 -> Step(
                icon = Icons.Rounded.Tune,
                title = controller.s("ob_region_title"),
                text = controller.s("ob_region_text"),
            )
            else -> Step(
                icon = Icons.Rounded.PowerSettingsNew,
                title = controller.s("ob_done_title"),
                text = controller.s("ob_done_text"),
            )
        }

        Spacer(Modifier.weight(1f))
        ActionButton(
            text = when (step) {
                2 -> controller.s("yes_please")
                3 -> controller.s("ob_done_action")
                else -> controller.s("next")
            },
        ) {
            if (step == 3) onDone() else step++
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = when (step) {
                0 -> controller.s("skip")
                1 -> controller.s("ob_no_sub")
                2 -> controller.s("skip")
                else -> ""
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { if (step == 3) onDone() else step = 3 },
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Step(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(120.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(52.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Экран добавления подписки (ServersKt.AddSubscriptionScreen). */
@Composable
fun AddSubscriptionScreen(controller: AppController, onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    SubScreen(title = controller.s("add_title"), onBack = onBack) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                controller.s("add_hint"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            AppTextField(
                value = text,
                onValueChange = { text = it; error = null },
                label = controller.s("add_field"),
                placeholder = "https://…",
                singleLine = false,
                minLines = 3,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable {
                        val clip = controller.platform.clipboardText()
                        if (clip.isNullOrBlank()) {
                            error = controller.s("add_clipboard_empty")
                        } else {
                            text = clip
                            error = null
                        }
                    },
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.size(18.dp))
                        Text(controller.s("add_paste"), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                MessageBanner(it, error = true)
            }

            Spacer(Modifier.height(20.dp))
            if (controller.busy) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(10.dp))
                    Text(controller.s("add_loading"), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                ActionButton(text = controller.s("add_action"), enabled = text.isNotBlank()) {
                    controller.addFromText(text) { ok ->
                        if (ok) {
                            text = ""
                            onBack()
                        } else {
                            error = controller.message
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                controller.s("add_manual"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                controller.s("ob_no_sub_text"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
