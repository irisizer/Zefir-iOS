package dev.rooni.zefir.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.app.AppController
import dev.rooni.zefir.ui.ZefirBadge

/**
 * Онбординг (ui/screens/Onboarding.kt): 3 шага — приветствие, добавление подписки, готово.
 * Шаги и тексты совпадают с оригиналом (ob_* в ресурсах).
 */
@Composable
fun OnboardingScreen(controller: AppController) {
    var step by remember { mutableStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp),
    ) {
        StepProgress(step = step, steps = 3, modifier = Modifier.padding(top = 18.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (step) {
                0 -> StepContent(
                    icon = null,
                    title = controller.s("ob_welcome_title"),
                    text = controller.s("ob_welcome_text"),
                )
                1 -> StepContent(
                    icon = true,
                    title = controller.s("ob_add_title"),
                    text = controller.s("ob_add_text"),
                )
                else -> StepContent(
                    icon = true,
                    done = true,
                    title = controller.s("ob_done_title"),
                    text = controller.s("ob_done_text"),
                )
            }
        }
        Button(
            onClick = {
                if (step < 2) step++ else controller.completeOnboarding()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 0.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                if (step < 2) controller.s("next") else controller.s("ob_done_action"),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (step == 1) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { controller.completeOnboarding() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(controller.s("ob_no_sub"), style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepProgress(step: Int, steps: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // «волна» слева — как логотип-росчерк в оригинале
        Canvas(Modifier.size(width = 64.dp, height = 16.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, size.height * 0.75f)
                cubicTo(size.width * 0.2f, 0f, size.width * 0.35f, size.height, size.width * 0.5f, size.height * 0.4f)
                cubicTo(size.width * 0.65f, 0f, size.width * 0.8f, size.height, size.width, size.height * 0.3f)
            }
            drawPath(path, primary, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
        Canvas(Modifier.weight(1f).height(16.dp)) {
            val y = size.height / 2
            drawLine(track, Offset(0f, y), Offset(size.width * 0.97f, y), strokeWidth = 2f)
            drawCircle(if (step >= steps - 1) primary else track, radius = 3.5f, center = Offset(size.width - 2f, y))
        }
    }
}

@Composable
private fun StepContent(
    icon: Boolean?,
    title: String,
    text: String,
    done: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            ZefirBadge(
                size = 160.dp,
                icon = if (done) Icons.Rounded.Check else Icons.Rounded.Link,
                iconTint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 36.dp),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/** Общая шапка подэкранов (Components.kt: SubScreen). */
@Composable
fun SubScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (onBack != null) {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    androidx.compose.material3.Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            actions?.invoke()
        }
        content()
    }
}