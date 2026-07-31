package com.clamit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================================
// clamit · Material 3 Expressive — locked design system (see DESIGN.md)
// Warm bone paper · deep teal ink · amber signal. No lavender, no pastel slop.
// ============================================================================

object ClamitColors {
    // Paper & ink
    val PaperLight = Color(0xFFF6F4EF)
    val PaperDark = Color(0xFF111412)
    val InkLight = Color(0xFF1C2321)
    val InkDark = Color(0xFFE4E7E2)
    val MutedLight = Color(0xFF5A615D)
    val MutedDark = Color(0xFFA8AEAA)

    // Primary teal
    val Teal = Color(0xFF0E5C52)
    val TealBright = Color(0xFF8BD6C7)
    val OnTeal = Color(0xFFFFFFFF)
    val TealContainerLight = Color(0xFFB9E8DD)
    val TealContainerDark = Color(0xFF1E5A50)
    val OnTealContainerLight = Color(0xFF06302B)
    val OnTealContainerDark = Color(0xFFB9E8DD)

    // Secondary slate
    val SlateLight = Color(0xFF4A5E5A)
    val SlateDark = Color(0xFFB1CCC4)
    val SlateContainerLight = Color(0xFFD5E5DF)
    val SlateContainerDark = Color(0xFF334C46)

    // Tertiary clay
    val ClayLight = Color(0xFF7A5A31)
    val ClayDark = Color(0xFFE4C28B)
    val ClayContainerLight = Color(0xFFF3E0C0)
    val ClayContainerDark = Color(0xFF5E4423)

    // Signal colors
    val SignalAmber = Color(0xFFC2641B)      // in_progress
    val SignalAmberDark = Color(0xFFFFB77C)
    val SignalAmberBg = Color(0xFFFBE3C8)    // in_progress container
    val SignalAmberBgDark = Color(0xFF4A2C12)
    val CompletedTeal = Color(0xFF1B7A5E)     // completed
    val CompletedTealDark = Color(0xFF7BD8AE)
    val CompletedBg = Color(0xFFDCEFE6)       // completed container
    val CompletedBgDark = Color(0xFF16382C)
    val PendingTeal = Color(0xFF4C7A72)       // pending
    val PendingTealDark = Color(0xFF8FC0B7)
    val PendingBg = Color(0xFFE4ECE9)         // pending container
    val PendingBgDark = Color(0xFF1E2E2B)

    // Surfaces (warm neutrals)
    val SurfaceLight = Color(0xFFF6F4EF)
    val SurfaceContainerLowLight = Color(0xFFF0EDE6)
    val SurfaceContainerLight = Color(0xFFEBE8E1)
    val SurfaceContainerHighLight = Color(0xFFE5E2DA)
    val SurfaceDark = Color(0xFF111412)
    val SurfaceContainerLowDark = Color(0xFF181B19)
    val SurfaceContainerDark = Color(0xFF1E211E)
    val SurfaceContainerHighDark = Color(0xFF2A2D2A)

    // Lines & error
    val HairlineLight = Color(0xFFCBC8BE)
    val HairlineDark = Color(0xFF3A403D)
    val ErrorLight = Color(0xFFBA1A1A)
    val ErrorDark = Color(0xFFFFB4AB)
    val ErrorBgLight = Color(0xFFFFDAD6)
    val ErrorBgDark = Color(0xFF5C1A12)
}

private val ClamitLightColors = lightColorScheme(
    primary = ClamitColors.Teal,
    onPrimary = ClamitColors.OnTeal,
    primaryContainer = ClamitColors.TealContainerLight,
    onPrimaryContainer = ClamitColors.OnTealContainerLight,
    secondary = ClamitColors.SlateLight,
    secondaryContainer = ClamitColors.SlateContainerLight,
    tertiary = ClamitColors.ClayLight,
    tertiaryContainer = ClamitColors.ClayContainerLight,
    background = ClamitColors.PaperLight,
    onBackground = ClamitColors.InkLight,
    surface = ClamitColors.SurfaceLight,
    onSurface = ClamitColors.InkLight,
    surfaceVariant = ClamitColors.SurfaceContainerLight,
    onSurfaceVariant = ClamitColors.MutedLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = ClamitColors.SurfaceContainerLowLight,
    surfaceContainer = ClamitColors.SurfaceContainerLight,
    surfaceContainerHigh = ClamitColors.SurfaceContainerHighLight,
    surfaceContainerHighest = Color(0xFFDEDBD3),
    outline = ClamitColors.MutedLight,
    outlineVariant = ClamitColors.HairlineLight,
    error = ClamitColors.ErrorLight,
    errorContainer = ClamitColors.ErrorBgLight
)

private val ClamitDarkColors = darkColorScheme(
    primary = ClamitColors.TealBright,
    onPrimary = Color(0xFF06302B),
    primaryContainer = ClamitColors.TealContainerDark,
    onPrimaryContainer = ClamitColors.OnTealContainerDark,
    secondary = ClamitColors.SlateDark,
    secondaryContainer = ClamitColors.SlateContainerDark,
    tertiary = ClamitColors.ClayDark,
    tertiaryContainer = ClamitColors.ClayContainerDark,
    background = ClamitColors.PaperDark,
    onBackground = ClamitColors.InkDark,
    surface = ClamitColors.SurfaceDark,
    onSurface = ClamitColors.InkDark,
    surfaceVariant = ClamitColors.SurfaceContainerDark,
    onSurfaceVariant = ClamitColors.MutedDark,
    surfaceContainerLowest = Color(0xFF0C0F0D),
    surfaceContainerLow = ClamitColors.SurfaceContainerLowDark,
    surfaceContainer = ClamitColors.SurfaceContainerDark,
    surfaceContainerHigh = ClamitColors.SurfaceContainerHighDark,
    surfaceContainerHighest = Color(0xFF353936),
    outline = ClamitColors.MutedDark,
    outlineVariant = ClamitColors.HairlineDark,
    error = ClamitColors.ErrorDark,
    errorContainer = ClamitColors.ErrorBgDark
)

@Composable
fun ClamitTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ClamitDarkColors else ClamitLightColors,
        content = content
    )
}
