package dev.rooni.zefir.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Генератор tonal-палитры из seed-цвета — та же идея, что material-kolor (PaletteStyle.Fidelity)
 * в оригинале: сохраняем оттенок seed, берём тона Material 3.
 *
 * Своя реализация нужна потому, что material-kolor на desktop-таргете под Xvfb отдаёт
 * вырожденную (чёрно-серую) схему, а нам нужен предсказуемый результат и на iOS, и на JVM.
 */
object TonalScheme {

    fun singleHue(seed: Long, dark: Boolean): ColorScheme {
        val hsv = rgbToHsv(seed)
        val hue = hsv[0]
        val sat = hsv[1]
        val accentSat = (sat * 0.85f).coerceIn(0.25f, 0.9f)

        fun tone(value: Float, s: Float = accentSat): Color = hsvToColor(hue, s, value)

        return if (dark) {
            darkColorScheme(
                primary = tone(0.78f),
                onPrimary = tone(0.20f, 0.5f),
                primaryContainer = tone(0.32f),
                onPrimaryContainer = tone(0.90f, 0.35f),
                secondary = tone(0.72f, 0.35f),
                onSecondary = tone(0.22f, 0.3f),
                secondaryContainer = tone(0.28f, 0.3f),
                onSecondaryContainer = tone(0.90f, 0.25f),
                tertiary = tone(0.70f, 0.30f),
                onTertiary = tone(0.20f, 0.3f),
                tertiaryContainer = tone(0.26f, 0.3f),
                onTertiaryContainer = tone(0.88f, 0.25f),
                background = tone(0.06f, 0.05f),
                onBackground = tone(0.92f, 0.06f),
                surface = tone(0.06f, 0.05f),
                onSurface = tone(0.93f, 0.05f),
                surfaceVariant = tone(0.24f, 0.08f),
                onSurfaceVariant = tone(0.78f, 0.10f),
                surfaceContainerLowest = tone(0.03f, 0.05f),
                surfaceContainerLow = tone(0.09f, 0.06f),
                surfaceContainer = tone(0.12f, 0.06f),
                surfaceContainerHigh = tone(0.16f, 0.06f),
                surfaceContainerHighest = tone(0.20f, 0.06f),
                outline = tone(0.55f, 0.10f),
                outlineVariant = tone(0.30f, 0.08f),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6),
            )
        } else {
            lightColorScheme(
                primary = tone(0.32f),
                onPrimary = Color.White,
                primaryContainer = tone(0.88f, 0.45f),
                onPrimaryContainer = tone(0.18f),
                secondary = tone(0.42f, 0.30f),
                onSecondary = Color.White,
                secondaryContainer = tone(0.90f, 0.30f),
                onSecondaryContainer = tone(0.22f, 0.35f),
                tertiary = tone(0.45f, 0.25f),
                onTertiary = Color.White,
                tertiaryContainer = tone(0.90f, 0.25f),
                onTertiaryContainer = tone(0.20f, 0.30f),
                background = tone(0.98f, 0.04f),
                onBackground = tone(0.14f, 0.06f),
                surface = tone(0.98f, 0.04f),
                onSurface = tone(0.14f, 0.06f),
                surfaceVariant = tone(0.90f, 0.06f),
                onSurfaceVariant = tone(0.38f, 0.08f),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = tone(0.97f, 0.04f),
                surfaceContainer = tone(0.94f, 0.05f),
                surfaceContainerHigh = tone(0.92f, 0.05f),
                surfaceContainerHighest = tone(0.89f, 0.05f),
                outline = tone(0.55f, 0.08f),
                outlineVariant = tone(0.82f, 0.06f),
                error = Color(0xFFBA1A1A),
                onError = Color.White,
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002),
            )
        }
    }

    private fun rgbToHsv(argb: Long): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val maxV = max(r, max(g, b))
        val minV = min(r, min(g, b))
        val d = maxV - minV
        var h = when {
            d == 0f -> 0f
            maxV == r -> ((g - b) / d).let { if (it < 0) it + 6f else it }
            maxV == g -> ((b - r) / d) + 2f
            else -> ((r - g) / d) + 4f
        } / 6f
        val s = if (maxV == 0f) 0f else d / maxV
        return floatArrayOf(h, s, maxV)
    }

    private fun hsvToColor(h: Float, s: Float, v: Float): Color {
        val c = v * s
        val x = c * (1 - abs((h * 6f) % 2 - 1))
        val m = v - c
        val (r, g, b) = when {
            h < 1f / 6f -> Triple(c, x, 0f)
            h < 2f / 6f -> Triple(x, c, 0f)
            h < 3f / 6f -> Triple(0f, c, x)
            h < 4f / 6f -> Triple(0f, x, c)
            h < 5f / 6f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (r + m).coerceIn(0f, 1f),
            green = (g + m).coerceIn(0f, 1f),
            blue = (b + m).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }
}