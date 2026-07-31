package com.clamit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material 3 Expressive Palette - Teal & Coral Theme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF70F5E8),
    onPrimaryContainer = Color(0xFF00201D),

    secondary = Color(0xFF4A6360),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E3),
    onSecondaryContainer = Color(0xFF051F1D),

    tertiary = Color(0xFF984061),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3E001D),

    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF4F7F6),
    onBackground = Color(0xFF161D1C),

    surface = Color(0xFFF4F7F6),
    onSurface = Color(0xFF161D1C),

    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF2F1),
    surfaceContainer = Color(0xFFE8ECEB),
    surfaceContainerHigh = Color(0xFFE2E6E5),
    surfaceContainerHighest = Color(0xFFDCE0DF)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF52DBCB),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF70F5E8),

    secondary = Color(0xFFB1CCC7),
    onSecondary = Color(0xFF1C3532),
    secondaryContainer = Color(0xFF334B48),
    onSecondaryContainer = Color(0xFFCCE8E3),

    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF5E1133),
    tertiaryContainer = Color(0xFF7B2949),
    onTertiaryContainer = Color(0xFFFFD9E2),

    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDCE0DF),

    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDCE0DF),

    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120),
    surfaceContainerHigh = Color(0xFF252C2B),
    surfaceContainerHighest = Color(0xFF303736)
)

@Composable
fun ClamitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
