package com.app.gectyping.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

// ============================================================
// Game-specific color set — consumed via LocalGameColors
// ============================================================
@Stable
data class GameColors(
    val background: Color,
    val cardBackground: Color,
    val buttonBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val overlay: Color,
    val surface: Color,
    val border: Color,
    val accent: Color,
    val isDark: Boolean
)

/** Build a GameColors from a WorldPalette */
fun WorldPalette.toGameColors() = GameColors(
    background       = background,
    cardBackground   = card,
    buttonBackground = button,
    textPrimary      = textPrimary,
    textSecondary    = textSecondary,
    overlay          = overlay,
    surface          = surface,
    border           = border,
    accent           = accent,
    isDark           = isDark
)

val DarkGameColors  = SpacePalette.toGameColors()
val LightGameColors = SpacePalette.toGameColors()

val LocalGameColors = compositionLocalOf { DarkGameColors }

// Animation duration for smooth theme fade (0.7 sec per spec)
private const val THEME_ANIM_MS = 700

@Composable
fun GecTypingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeWorld: ThemeWorld = if (darkTheme) ThemeWorld.SPACE else ThemeWorld.BOG,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = worldPalettes[themeWorld] ?: SpacePalette
    val targetGameColors = palette.toGameColors()

    val materialScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent, secondary = RedArc, tertiary = YellowStar,
            background = palette.background, surface = palette.card,
            onPrimary = White, onSecondary = White,
            onBackground = palette.textPrimary, onSurface = palette.textPrimary
        )
    } else {
        lightColorScheme(
            primary = palette.accent, secondary = RedArc, tertiary = YellowStar,
            background = palette.background, surface = palette.card,
            onPrimary = White, onSecondary = White,
            onBackground = palette.textPrimary, onSurface = palette.textPrimary
        )
    }

    // Smoothly animate every game color on theme switch (0.7s fade)
    val animSpec = tween<Color>(THEME_ANIM_MS)
    val bg        by animateColorAsState(targetGameColors.background,       animSpec, label = "bg")
    val card      by animateColorAsState(targetGameColors.cardBackground,   animSpec, label = "card")
    val btn       by animateColorAsState(targetGameColors.buttonBackground, animSpec, label = "btn")
    val txtPri    by animateColorAsState(targetGameColors.textPrimary,      animSpec, label = "txtPri")
    val txtSec    by animateColorAsState(targetGameColors.textSecondary,    animSpec, label = "txtSec")
    val overlay   by animateColorAsState(targetGameColors.overlay,          animSpec, label = "overlay")
    val surface   by animateColorAsState(targetGameColors.surface,          animSpec, label = "surface")
    val bdr       by animateColorAsState(targetGameColors.border,           animSpec, label = "bdr")
    val acc       by animateColorAsState(targetGameColors.accent,           animSpec, label = "acc")

    val animatedGameColors = GameColors(
        background       = bg,
        cardBackground   = card,
        buttonBackground = btn,
        textPrimary      = txtPri,
        textSecondary    = txtSec,
        overlay          = overlay,
        surface          = surface,
        border           = bdr,
        accent           = acc,
        isDark           = palette.isDark
    )

    CompositionLocalProvider(LocalGameColors provides animatedGameColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography,
            content = content
        )
    }
}