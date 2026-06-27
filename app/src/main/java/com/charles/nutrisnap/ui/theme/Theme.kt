package com.charles.nutrisnap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extra brand colors that don't map onto Material's [androidx.compose.material3.ColorScheme]
 * (macro hues, tints, the candy "pop" shadow). Access via [NutriTheme.colors].
 */
@Immutable
data class NutriColors(
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val info: Color,
    val streak: Color,
    val popShadow: Color,
    val ringTrack: Color,
    val mangoTint: Color,
    val mintTint: Color,
    val skyTint: Color,
    val berryTint: Color,
)

private val LightNutriColors = NutriColors(
    protein = Mint, carbs = Grape, fat = Butter, info = Sky,
    streak = MangoDeep, popShadow = MangoDeep, ringTrack = RingTrack,
    mangoTint = MangoTint, mintTint = MintTint, skyTint = SkyTint,
    berryTint = BerryTint,
)

private val DarkNutriColors = LightNutriColors.copy(
    ringTrack = Color(0xFF3A2D24),
    mangoTint = Color(0xFF4A3717),
    mintTint = Color(0xFF173A33),
    skyTint = Color(0xFF153039),
    berryTint = Color(0xFF4A1722),
)

val LocalNutriColors = staticCompositionLocalOf { LightNutriColors }

private val LightColors = lightColorScheme(
    primary = Mango,
    onPrimary = Color.White,
    primaryContainer = MangoTint,
    onPrimaryContainer = MangoDeep,
    secondary = Berry,
    onSecondary = Color.White,
    secondaryContainer = BerryTint,
    onSecondaryContainer = BerryDeep,
    tertiary = Mint,
    onTertiary = Color.White,
    background = Cream,
    onBackground = Cocoa,
    surface = CreamCard,
    onSurface = Cocoa,
    surfaceVariant = MangoTint,
    onSurfaceVariant = CocoaSoft,
    outline = RingTrack,
    error = BerryDeep,
)

private val DarkColors = darkColorScheme(
    primary = Mango,
    onPrimary = Color(0xFF2A1B0C),
    primaryContainer = Color(0xFF4A3717),
    onPrimaryContainer = Butter,
    secondary = Berry,
    onSecondary = Color(0xFF3A0E16),
    tertiary = Mint,
    background = Color(0xFF1E1712),
    onBackground = Color(0xFFF3E7DA),
    surface = Color(0xFF2A211A),
    onSurface = Color(0xFFF3E7DA),
    surfaceVariant = Color(0xFF3A2D24),
    onSurfaceVariant = Color(0xFFC9B6A6),
    outline = Color(0xFF4A3B30),
    error = Berry,
)

@Composable
fun NutriSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val nutriColors = if (darkTheme) DarkNutriColors else LightNutriColors

    CompositionLocalProvider(LocalNutriColors provides nutriColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}

/** Convenience accessor for brand-specific colors, mirroring [MaterialTheme]. */
object NutriTheme {
    val colors: NutriColors
        @Composable @ReadOnlyComposable get() = LocalNutriColors.current
}
