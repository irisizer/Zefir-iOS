package dev.rooni.zefir.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Логотип-«печать» из оригинала (ui/ZefirMark.kt): круг с 12 лепестками.
 * Рисуем через Canvas, чтобы одинаково работало на iOS и десктопе.
 */
@Composable
fun ZefirBadge(
    size: Dp = 160.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            drawScallop(color, lobes = 12)
        }
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.42f))
        }
    }
}

private fun DrawScope.drawScallop(color: Color, lobes: Int) {
    val r = this.size.minDimension / 2f
    val center = Offset(this.size.width / 2f, this.size.height / 2f)
    val lobeR = r * 0.28f
    val ring = r - lobeR
    drawCircle(color, radius = ring * 0.92f, center = center)
    for (i in 0 until lobes) {
        val angle = (2.0 * Math.PI * i / lobes).toFloat()
        val c = Offset(center.x + ring * cos(angle), center.y + ring * sin(angle))
        drawCircle(color, radius = lobeR, center = c)
    }
}

/** Маленький «лист»/капля внутри значка (как в оригинале). */
@Composable
fun ZefirLeaves(color: Color, size: Dp = 64.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.06f, h * 0.3f),
            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.08f),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.36f, h * 0.52f),
            size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.08f),
        )
    }
}