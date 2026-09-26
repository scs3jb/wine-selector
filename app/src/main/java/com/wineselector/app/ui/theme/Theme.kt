package com.wineselector.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Bordeaux,
    onPrimary = Color.White,
    primaryContainer = Claret,
    onPrimaryContainer = BordeauxDark,
    secondary = Cork,
    onSecondary = Color.White,
    secondaryContainer = ParchmentDim,
    onSecondaryContainer = Ink,
    tertiary = Gold,
    onTertiary = Ink,
    tertiaryContainer = GoldLight,
    onTertiaryContainer = Ink,
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = ParchmentDim,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF3EE),
    surfaceContainer = Color(0xFFFBEDE7),
    surfaceContainerHigh = Color(0xFFF5E6E0),
    surfaceContainerHighest = Color(0xFFEFE0DA),
    outline = Color(0xFF8C7A7D),
    outlineVariant = Color(0xFFDCCBC6),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C0),
    onPrimary = BordeauxDark,
    primaryContainer = Bordeaux,
    onPrimaryContainer = Claret,
    secondary = Color(0xFFE2C1A6),
    onSecondary = Color(0xFF3F2A1B),
    secondaryContainer = NightHigh,
    onSecondaryContainer = Color(0xFFF2DED4),
    tertiary = Color(0xFFE9C46A),
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF5A4513),
    onTertiaryContainer = GoldLight,
    background = Night,
    onBackground = Color(0xFFEFDFE1),
    surface = Night,
    onSurface = Color(0xFFEFDFE1),
    surfaceVariant = NightHigh,
    onSurfaceVariant = Color(0xFFD5C2C5),
    surfaceContainerLowest = Color(0xFF110C0E),
    surfaceContainerLow = Color(0xFF1C1517),
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = Color(0xFF392E31),
    outline = Color(0xFFA08C8F),
    outlineVariant = Color(0xFF524346),
    error = Color(0xFFFFB4AB)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun WineSelectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = Typography, shapes = AppShapes, content = content)
}
