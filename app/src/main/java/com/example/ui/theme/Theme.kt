package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Immutable
data class SandooqiColors(
    val emerald: Color,
    val emeraldContainer: Color,
    val onEmeraldContainer: Color,
    val amber: Color,
    val amberContainer: Color,
    val onAmberContainer: Color,
    val rose: Color,
    val roseContainer: Color,
    val onRoseContainer: Color,
    val violet: Color,
    val violetContainer: Color,
    val onVioletContainer: Color,
    val sky: Color,
    val skyContainer: Color,
    val onSkyContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroAccent: Color,
    val cardBorder: Color,
)

private val LightExtended = SandooqiColors(
    emerald = Emerald700,
    emeraldContainer = Emerald50,
    onEmeraldContainer = Emerald900,
    amber = Amber600,
    amberContainer = Amber50,
    onAmberContainer = Color(0xFF78350F),
    rose = Rose600,
    roseContainer = Rose50,
    onRoseContainer = Color(0xFF881337),
    violet = Violet700,
    violetContainer = Violet50,
    onVioletContainer = Color(0xFF4C1D95),
    sky = Sky700,
    skyContainer = Sky50,
    onSkyContainer = Color(0xFF0C4A6E),
    heroStart = Emerald700,
    heroEnd = Emerald900,
    heroAccent = Amber300,
    cardBorder = LightOutline,
)

private val DarkExtended = SandooqiColors(
    emerald = Color(0xFF34D399),
    emeraldContainer = Color(0xFF064E3B),
    onEmeraldContainer = Color(0xFFA7F3D0),
    amber = Amber300,
    amberContainer = Color(0xFF451A03),
    onAmberContainer = Color(0xFFFDE68A),
    rose = Rose400,
    roseContainer = Color(0xFF4C0519),
    onRoseContainer = Color(0xFFFECDD3),
    violet = Violet400,
    violetContainer = Color(0xFF2E1065),
    onVioletContainer = Color(0xFFDDD6FE),
    sky = Sky400,
    skyContainer = Color(0xFF082F49),
    onSkyContainer = Color(0xFFBAE6FD),
    heroStart = Color(0xFF065F46),
    heroEnd = Color(0xFF022C22),
    heroAccent = Amber300,
    cardBorder = DarkOutline,
)

val LocalSandooqiColors = staticCompositionLocalOf { LightExtended }

private val LightColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Amber600,
    onSecondary = Color.White,
    secondaryContainer = Amber100,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Violet700,
    onTertiary = Color.White,
    background = CreamBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = Rose600,
    onError = Color.White,
    errorContainer = Rose100,
    onErrorContainer = Color(0xFF881337),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald100,
    secondary = Amber300,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Amber100,
    tertiary = Violet400,
    onTertiary = Color(0xFF2E1065),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = Rose400,
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF881337),
    onErrorContainer = Rose100,
)

object SandooqiThemeTokens {
    val colors: SandooqiColors
        @Composable get() = LocalSandooqiColors.current
}

@Composable
fun SandooqiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkExtended else LightExtended

    CompositionLocalProvider(
        LocalSandooqiColors provides extended,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SandooqiTypography,
            content = content,
        )
    }
}
