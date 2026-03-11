package com.app.gectyping.ui.theme

import androidx.compose.ui.graphics.Color
import com.app.gectyping.R

// ============================================================
// SHARED / ACCENT COLORS (same in all themes)
// ============================================================
val LightGray = Color(0xFFB0B0B0)
val White = Color(0xFFFFFFFF)
val GreenSubmit = Color(0xFF4CAF50)
val OrangeFlame = Color(0xFFFF9800)
val YellowStar = Color(0xFFFFD700)
val YellowCoin = Color(0xFFFFC107)
val BlueG = Color(0xFF2196F3)
val RedArc = Color(0xFFF44336)

// Legacy colors for compatibility
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ============================================================
// LUMINANCE HELPER — determines if a color is "dark"
// ============================================================
fun Color.luminance(): Float {
    val r = red; val g = green; val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

// ============================================================
// 10 THEMATIC WORLDS
// ============================================================

/** Unique identifier for each theme world */
enum class ThemeWorld(val displayName: String, val iconRes: Int? = null, val emoji: String) {
    BOG("Chameleon", R.drawable.icons8_chameleon_96, "🦉"),
    SPACE("Space",       R.drawable.icons8_rocket_64, "🚀"),
    FOREST("Forest",     R.drawable.icons8_forest_96, "🌲"),
    OCEAN("Island",       R.drawable.icons8_island_on_water_96, "🌊"),
    GAMING("Gaming",     R.drawable.icons8_game_96, "🎮"),
    DESERT("Desert",     R.drawable.icons8_desert_plant_96, "🏜️"),
    ICE("Snow",           R.drawable.icons8_snow_96, "❄️"),

    CYBER("Robot",       R.drawable.icons8_music_robot_96, "🤖"),
    MAGMA("Volcano",       R.drawable.volcano,  "🌋"),

    SAKURA("Sakura",     R.drawable.icons8_sakura_96, "🌸");

    companion object {
        fun fromId(id: String): ThemeWorld =
            entries.firstOrNull { it.name == id } ?: SPACE
    }
}

/** Data class holding all colors for one world */
data class WorldPalette(
    val background: Color,
    val card: Color,
    val button: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val surface: Color,
    val overlay: Color,
    val border: Color,
    val accent: Color,
    val isDark: Boolean
)

// 0. BOG — Official BOG colors (Dark mode)
val BOGPalette = WorldPalette(
    background    = Color(0xFF131F24),  // BOG dark background
    card          = Color(0xFF1A2C32),  // Slightly lighter card
    button        = Color(0xFF58CC02),  // BOG green
    textPrimary   = Color(0xFFFFFFFF),  // White text
    textSecondary = Color(0xFFAFAFAF),  // Gray text
    surface       = Color(0xFF1A2C32),
    overlay       = Color(0xFF0D1518),
    border        = Color(0xFF37464F),
    accent        = Color(0xFF58CC02),  // BOG green
    isDark        = true
)

// 1. SPACE — Deep midnight blue, neon glow
val SpacePalette = WorldPalette(
    background    = Color(0xFF020617),
    card          = Color(0xFF0F172A),
    button        = Color(0xFF1E293B),
    textPrimary   = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    surface       = Color(0xFF0F172A),
    overlay       = Color(0xFF020617),
    border        = Color(0xFF334155),
    accent        = Color(0xFF38BDF8),  // neon cyan
    isDark        = true
)

// 2. FOREST — Dark pine green
val ForestPalette = WorldPalette(
    background    = Color(0xFF064E3B),
    card          = Color(0xFF065F46),
    button        = Color(0xFF047857),
    textPrimary   = Color(0xFFFEF3C7),  // warm beige
    textSecondary = Color(0xFFA7F3D0),
    surface       = Color(0xFF065F46),
    overlay       = Color(0xFF022C22),
    border        = Color(0xFF059669),
    accent        = Color(0xFF34D399),
    isDark        = true
)

// 3. OCEAN — Rich azure
val OceanPalette = WorldPalette(
    background    = Color(0xFF075985),
    card          = Color(0xFF0369A1),
    button        = Color(0xFF0284C7),
    textPrimary   = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFBAE6FD),
    surface       = Color(0xFF0369A1),
    overlay       = Color(0xFF0C4A6E),
    border        = Color(0xFF0EA5E9),
    accent        = Color(0xFF38BDF8),
    isDark        = true
)

// 4. GAMING — Dark purple, acid green buttons
val GamingPalette = WorldPalette(
    background    = Color(0xFF4C1D95),
    card          = Color(0xFF5B21B6),
    button        = Color(0xFF6D28D9),
    textPrimary   = Color(0xFFF5F3FF),
    textSecondary = Color(0xFFC4B5FD),
    surface       = Color(0xFF5B21B6),
    overlay       = Color(0xFF3B0764),
    border        = Color(0xFF7C3AED),
    accent        = Color(0xFF4ADE80),  // acid green
    isDark        = true
)

// 5. DESERT — Warm sand
val DesertPalette = WorldPalette(
    background    = Color(0xFFFEF3C7),
    card          = Color(0xFFFDE68A),
    button        = Color(0xFFFCD34D),
    textPrimary   = Color(0xFF78350F),
    textSecondary = Color(0xFF92400E),
    surface       = Color(0xFFFDE68A),
    overlay       = Color(0xFFFFFBEB),
    border        = Color(0xFFF59E0B),
    accent        = Color(0xFFD97706),
    isDark        = false
)

// 6. CYBER — Black + hot pink
val CyberPalette = WorldPalette(
    background    = Color(0xFF0A0A0A),
    card          = Color(0xFF171717),
    button        = Color(0xFF262626),
    textPrimary   = Color(0xFFFCE7F3),
    textSecondary = Color(0xFFFB7185),
    surface       = Color(0xFF171717),
    overlay       = Color(0xFF000000),
    border        = Color(0xFF831843),
    accent        = Color(0xFFBE185D),  // hot pink
    isDark        = true
)

// 7. MINIMAL — Pure white, black text
val MinimalPalette = WorldPalette(
    background    = Color(0xFFFFFFFF),
    card          = Color(0xFFF5F5F5),
    button        = Color(0xFFE5E5E5),
    textPrimary   = Color(0xFF0A0A0A),
    textSecondary = Color(0xFF525252),
    surface       = Color(0xFFF5F5F5),
    overlay       = Color(0xFFFAFAFA),
    border        = Color(0xFFD4D4D4),
    accent        = Color(0xFF171717),
    isDark        = false
)

// 8. SAKURA — Soft pink, dark burgundy text
val SakuraPalette = WorldPalette(
    background    = Color(0xFFFBCFE8),
    card          = Color(0xFFFCE7F3),
    button        = Color(0xFFF9A8D4),
    textPrimary   = Color(0xFF831843),
    textSecondary = Color(0xFF9D174D),
    surface       = Color(0xFFFCE7F3),
    overlay       = Color(0xFFFDF2F8),
    border        = Color(0xFFF472B6),
    accent        = Color(0xFFDB2777),
    isDark        = false
)

// 9. ICE — Light icy blue
val IcePalette = WorldPalette(
    background    = Color(0xFFE0F2FE),
    card          = Color(0xFFF0F9FF),
    button        = Color(0xFFBAE6FD),
    textPrimary   = Color(0xFF0C4A6E),
    textSecondary = Color(0xFF075985),
    surface       = Color(0xFFF0F9FF),
    overlay       = Color(0xFFF0F9FF),
    border        = Color(0xFF7DD3FC),
    accent        = Color(0xFF0284C7),
    isDark        = false
)

// 10. MAGMA — Dark charcoal + flame orange
val MagmaPalette = WorldPalette(
    background    = Color(0xFF1C1917),
    card          = Color(0xFF292524),
    button        = Color(0xFF44403C),
    textPrimary   = Color(0xFFFAFAF9),
    textSecondary = Color(0xFFA8A29E),
    surface       = Color(0xFF292524),
    overlay       = Color(0xFF0C0A09),
    border        = Color(0xFF57534E),
    accent        = Color(0xFFF97316),  // flame orange
    isDark        = true
)

/** Map from ThemeWorld to its palette */
val worldPalettes: Map<ThemeWorld, WorldPalette> = mapOf(
    ThemeWorld.BOG to BOGPalette,
    ThemeWorld.SPACE   to SpacePalette,
    ThemeWorld.FOREST  to ForestPalette,
    ThemeWorld.OCEAN   to OceanPalette,
    ThemeWorld.GAMING  to GamingPalette,
    ThemeWorld.DESERT  to DesertPalette,
    ThemeWorld.CYBER   to CyberPalette,
    ThemeWorld.SAKURA  to SakuraPalette,
    ThemeWorld.ICE     to IcePalette,
    ThemeWorld.MAGMA   to MagmaPalette,
)

/** Price in coins for each world theme (BOG is free/default) */
val worldPrices: Map<ThemeWorld, Int> = mapOf(
    ThemeWorld.BOG to 0,
    ThemeWorld.SPACE   to 0,
    ThemeWorld.FOREST  to 100,
    ThemeWorld.OCEAN   to 120,
    ThemeWorld.GAMING  to 150,
    ThemeWorld.DESERT  to 100,
    ThemeWorld.CYBER   to 180,
    ThemeWorld.SAKURA  to 120,
    ThemeWorld.ICE     to 100,
    ThemeWorld.MAGMA   to 160,
)

// Backward-compat aliases used by existing code
val DarkBlueBackground = SpacePalette.background
val DarkBlueCard       = SpacePalette.card
val DarkBlueButton     = SpacePalette.button
val DarkTextPrimary    = SpacePalette.textPrimary
val DarkTextSecondary  = SpacePalette.textSecondary
val DarkSurface        = SpacePalette.surface
val DarkOverlay        = SpacePalette.overlay
val LightBackground    = MinimalPalette.background
val LightCard          = MinimalPalette.card
val LightButton        = MinimalPalette.button
val LightTextPrimary   = MinimalPalette.textPrimary
val LightTextSecondary = MinimalPalette.textSecondary
val LightSurface       = MinimalPalette.surface
val LightOverlay       = MinimalPalette.overlay
val LightBorder        = MinimalPalette.border