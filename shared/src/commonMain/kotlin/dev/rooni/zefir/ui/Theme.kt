package dev.rooni.zefir.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.ThemeMode
import dev.rooni.zefir.platform.Platform

/** 12 акцентов из оригинала (ThemeKt.AccentChoices). */
val AccentChoices = listOf(
    0xFFC62828, 0xFFE64A19, 0xFFB5602A, 0xFF8D6E00,
    0xFF5F6F2A, 0xFF2F5D50, 0xFF00796B, 0xFF0277BD,
    0xFF3F5AA9, 0xFF5E35B1, 0xFF8E24AA, 0xFFAD1457,
)

/** Разноцветные палитры (ThemeKt.MultiPalettes): 5 оттенков на каждую. */
val MultiPalettes = listOf(
    listOf(0xFFE53935, 0xFFFB8C00, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA),
    listOf(0xFFC62828, 0xFFEF6C00, 0xFFAD1457, 0xFFF9A825, 0xFF6D4C41),
    listOf(0xFF0277BD, 0xFF00897B, 0xFF5E35B1, 0xFF2E7D32, 0xFF00ACC1),
    listOf(0xFF2E7D32, 0xFF8D6E00, 0xFF00796B, 0xFFB5602A, 0xFF5F6F2A),
    listOf(0xFF5E35B1, 0xFFD81B60, 0xFF1E88E5, 0xFF00ACC1, 0xFFFB8C00),
    listOf(0xFFAD1457, 0xFF7E57C2, 0xFFEC407A, 0xFF5C6BC0, 0xFFAB47BC),
)

data class IconTint(val icon: Color, val container: Color, val onContainer: Color)

val LocalIconTints = compositionLocalOf<List<IconTint>> {
    listOf(IconTint(Color.Unspecified, Color.Unspecified, Color.Unspecified))
}

/** Один тон из seed-цвета — Fallback, если material-kolor выдал вырожденную схему. */
fun singleHue(seed: Long, dark: Boolean): ColorScheme {
    val generated = runCatching {
        dynamicColorScheme(
            seedColor = Color(seed),
            isDark = dark,
            style = PaletteStyle.Fidelity,
        )
    }.getOrNull()
    if (generated != null) {
        val primary = generated.primary
        val valid = primary.red + primary.green + primary.blue > 0.05f
        if (valid) return generated
    }
    return fallbackScheme(seed, dark)
}

/** Ручная генерация палитры из акцента (если библиотека не справилась). */
fun fallbackScheme(seed: Long, dark: Boolean): ColorScheme {
    val base = Color(seed)
    val h = hueOf(base)
    val s = saturationOf(base).coerceIn(0.25f, 0.85f)
    fun tone(t: Float, sat: Float = s): Color = fromHsl(h, sat, if (dark) t else 1f - t)
    return if (dark) {
        darkColorScheme(
            primary = tone(0.80f, sat = s * 0.75f),
            onPrimary = tone(0.12f),
            primaryContainer = tone(0.32f),
            onPrimaryContainer = tone(0.90f, sat = s * 0.5f),
            secondary = tone(0.78f, sat = s * 0.35f),
            onSecondary = tone(0.12f),
            secondaryContainer = tone(0.30f, sat = s * 0.35f),
            onSecondaryContainer = tone(0.90f, sat = s * 0.25f),
            tertiary = tone(0.78f, sat = s * 0.5f),
            background = tone(0.06f, sat = 0.06f),
            onBackground = tone(0.92f, sat = 0.06f),
            surface = tone(0.06f, sat = 0.06f),
            onSurface = tone(0.92f, sat = 0.06f),
            surfaceVariant = tone(0.30f, sat = 0.12f),
            onSurfaceVariant = tone(0.80f, sat = 0.10f),
            surfaceContainerLowest = tone(0.04f, sat = 0.06f),
            surfaceContainerLow = tone(0.09f, sat = 0.07f),
            surfaceContainer = tone(0.12f, sat = 0.08f),
            surfaceContainerHigh = tone(0.16f, sat = 0.08f),
            surfaceContainerHighest = tone(0.22f, sat = 0.08f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = tone(0.60f, sat = 0.10f),
        )
    } else {
        lightColorScheme(
            primary = tone(0.32f, sat = s * 0.9f),
            onPrimary = Color.White,
            primaryContainer = tone(0.88f, sat = s * 0.55f),
            onPrimaryContainer = tone(0.12f),
            secondary = tone(0.36f, sat = s * 0.45f),
            onSecondary = Color.White,
            secondaryContainer = tone(0.90f, sat = s * 0.30f),
            onSecondaryContainer = tone(0.14f, sat = s * 0.3f),
            tertiary = tone(0.38f, sat = s * 0.55f),
            background = tone(0.98f, sat = s * 0.05f),
            onBackground = tone(0.10f, sat = s * 0.10f),
            surface = tone(0.98f, sat = s * 0.05f),
            onSurface = tone(0.10f, sat = s * 0.10f),
            surfaceVariant = tone(0.90f, sat = s * 0.15f),
            onSurfaceVariant = tone(0.32f, sat = s * 0.10f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = tone(0.96f, sat = s * 0.06f),
            surfaceContainer = tone(0.94f, sat = s * 0.08f),
            surfaceContainerHigh = tone(0.92f, sat = s * 0.10f),
            surfaceContainerHighest = tone(0.90f, sat = s * 0.12f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            outline = tone(0.55f, sat = s * 0.10f),
        )
    }
}

private fun hueOf(color: Color): Float {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val d = max - min
    if (d == 0f) return 0f
    val h = when (max) {
        r -> ((g - b) / d) % 6f
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    }
    return ((h * 60f) + 360f) % 360f
}

private fun saturationOf(color: Color): Float {
    val max = maxOf(color.red, color.green, color.blue)
    val min = minOf(color.red, color.green, color.blue)
    if (max == 0f) return 0f
    return (max - min) / max
}

private fun fromHsl(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = h / 60f
    val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
    val (r1, g1, b1) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
    )
}

/**
 * Тема приложения: акцент / системный цвет / разноцветные палитры + AMOLED.
 * Аналог ThemeKt.AppTheme — с анимацией цвета иконок по индексу виджета.
 */
@Composable
fun AppTheme(
    settings: AppSettings,
    platform: Platform,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val systemScheme = if (settings.dynamicColor) platform.systemColorScheme(dark) else null

    val scheme = remember(settings.accent, settings.palette, settings.amoled, dark, settings.dynamicColor, systemScheme) {
        var base = systemScheme ?: run {
            // iOS: системного цвета нет — «Цвет системы» честно откатывается к акценту.
            // Разноцветная палитра включается только явным выбором (palette >= 0).
            val palette = if (systemScheme == null && settings.dynamicColor) null else MultiPalettes.getOrNull(settings.palette)
            if (palette != null) {
                val schemes = palette.map { singleHue(it, dark) }
                val second = schemes[1]
                val third = schemes[2]
                schemes[0].copy(
                    primary = second.primary,
                    onPrimary = second.onPrimary,
                    primaryContainer = second.primaryContainer,
                    onPrimaryContainer = second.onPrimaryContainer,
                    tertiary = third.primary,
                    onTertiary = third.onTertiary,
                    tertiaryContainer = third.primaryContainer,
                    onTertiaryContainer = third.onTertiaryContainer,
                )
            } else {
                singleHue(settings.accent, dark)
            }
        }
        if (dark && settings.amoled) {
            base = base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainer = Color(0xFF111111),
                surfaceContainerHigh = Color(0xFF181818),
                surfaceContainerHighest = Color(0xFF1F1F1F),
            )
        }
        base
    }

    val tintSource = if (systemScheme != null) {
        listOf(IconTint(scheme.primary, scheme.primaryContainer, scheme.onPrimaryContainer))
    } else {
        val palette = MultiPalettes.getOrNull(settings.palette)
        if (palette != null) {
            palette.map {
                val s = singleHue(it, dark)
                IconTint(s.primary, s.primaryContainer, s.onPrimaryContainer)
            }
        } else {
            listOf(IconTint(scheme.primary, scheme.primaryContainer, scheme.onPrimaryContainer))
        }
    }

    val tints = List(5) { index -> tintSource[index % tintSource.size] }
    val animated = tints.map {
        IconTint(
            animatedColor(it.icon),
            animatedColor(it.container),
            animatedColor(it.onContainer),
        )
    }

    CompositionLocalProvider(LocalIconTints provides animated) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(12.dp),
                small = RoundedCornerShape(16.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(36.dp),
            ),
            content = content,
        )
    }
}

@Composable
private fun animatedColor(target: Color): Color {
    val state = animateColorAsState(target, label = "scheme")
    return state.value
}
