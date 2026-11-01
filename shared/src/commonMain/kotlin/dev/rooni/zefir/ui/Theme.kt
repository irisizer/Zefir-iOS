package dev.rooni.zefir.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.rooni.zefir.model.AppSettings
import dev.rooni.zefir.model.ThemeMode
import dev.rooni.zefir.platform.Platform

/** Палитра иконок (Theme.kt: IconTint). */
data class IconTint(val icon: Color, val container: Color, val onContainer: Color)

val LocalIconTints = staticCompositionLocalOf<List<IconTint>> { emptyList() }

/** Пресеты акцентов из оригинала (Theme.kt: AccentChoices). */
val AccentChoices: List<Long> = listOf(
    4291176488L, 4293282329L, 4290076714L, 4287458816L, 4284444458L,
    4281294160L, 4278221163L, 4278351805L, 4282342057L, 4284364209L,
    4287505578L, 4289533015L,
)

/** Многопветные схемы (Theme.kt: MultiPalettes). */
val MultiPalettes: List<List<Long>> = listOf(
    listOf(4293212469L, 4294675456L, 4282622023L, 4280191205L, 4287505578L),
    listOf(4291176488L, 4293880832L, 4289533015L, 4294551589L, 4285353025L),
    listOf(4278351805L, 4278225275L, 4284364209L, 4281236786L, 4278234305L),
    listOf(4281236786L, 4287458816L, 4278221163L, 4290076714L, 4284444458L),
    listOf(4284364209L, 4292352864L, 4280191205L, 4278234305L, 4294675456L),
    listOf(4289533015L, 4286470082L, 4293673082L, 4284246976L, 4289415100L),
)

private fun singleHue(seed: Long, dark: Boolean): ColorScheme = TonalScheme.singleHue(seed, dark)

private fun tint(scheme: ColorScheme) = IconTint(
    icon = scheme.primary,
    container = scheme.primaryContainer,
    onContainer = scheme.onPrimaryContainer,
)

fun multiHue(seeds: List<Long>, dark: Boolean): Pair<ColorScheme, List<IconTint>> {
    val parts = seeds.map { singleHue(it, dark) }
    val base = parts[0]
    val second = parts.getOrNull(1) ?: base
    val third = parts.getOrNull(2) ?: base
    val scheme = base.copy(
        secondary = second.primary,
        onSecondary = second.onPrimary,
        secondaryContainer = second.primaryContainer,
        onSecondaryContainer = second.onPrimaryContainer,
        tertiary = third.primary,
        onTertiary = third.onPrimary,
        tertiaryContainer = third.primaryContainer,
        onTertiaryContainer = third.onPrimaryContainer,
    )
    return scheme to parts.map { tint(it) }
}

/** Тёмная AMOLED-схема (Theme.kt: settings.amoled). */
private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF171717),
    surfaceContainerHighest = Color(0xFF1E1E1E),
)

private val ZefirShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AppTheme(settings: AppSettings, platform: Platform, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val system = if (settings.dynamicColor) platform.systemColorScheme(dark) else null
    val palette = MultiPalettes.getOrNull(settings.palette)
    val generated = remember(settings.accent, palette, dark, system) {
        when {
            system != null -> null
            palette != null -> multiHue(palette, dark)
            else -> singleHue(settings.accent, dark).let { it to listOf(tint(it)) }
        }
    }
    val base = system ?: generated?.first ?: if (dark) darkColorScheme() else lightColorScheme()
    val scheme = if (dark && settings.amoled) base.amoled() else base
    val tints = if (system != null) listOf(tint(system)) else generated?.second ?: listOf(tint(scheme))

    println("ZEFIR theme: accent=${settings.accent} dark=$dark primary=${scheme.primary} surface=${scheme.surface} container=${scheme.surfaceContainer}")

    CompositionLocalProvider(LocalIconTints provides tints) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            shapes = ZefirShapes,
            content = content,
        )
    }
}

/** Плавная смена цветов между темами (Theme.kt: animated). */
@Composable
fun Color.animated(target: Color): Color = animateColorAsState(target, tween(450), label = "scheme").value

@Composable
fun ZefirSurfaceColor(): Color = MaterialTheme.colorScheme.surface