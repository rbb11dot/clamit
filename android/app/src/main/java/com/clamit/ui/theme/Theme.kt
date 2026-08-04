package com.clamit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// clamit · Material 3 Expressive — design tokens (DESIGN.md)
//
// OWN-WORLD: warm bone paper under deep teal ink, amber for "in progress",
// teal owns completion. The theme is the expressive Material system: dynamic
// color on Android 12+, expressive light scheme elsewhere (alpha25 ships no
// expressive dark scheme, so dark falls back to darkColorScheme()).
//
// The expressive identity is carried by three overrides layered on top of the
// expressive defaults:
//   • ClamitTypography — roman type only, weight steps carry hierarchy; the
//     display scale is pulled tight so the day numeral can act as the hero.
//   • ClamitShapes — one rounded-corner family, radii grown for the
//     "single rail" language (cards / sheets read as connected stops).
//   • MotionScheme.expressive() — the Material expressive motion scheme for
//     every M3 component in the hierarchy.
//
// Status colors (DESIGN.md rule 2) are product semantics with no
// expressive-scheme equivalent, so they live in ClamitStatusColors — fixed
// light values, dark variants kept for callers that opt in.
// ============================================================================

/** Status semantics: completed teal, in_progress amber, pending teal (hollow). */
object ClamitStatusColors {
    // completed
    val CompletedTeal = Color(0xFF1B7A5E)
    val CompletedTealDark = Color(0xFF7BD8AE)
    val CompletedBg = Color(0xFFDCEFE6)
    val CompletedBgDark = Color(0xFF16382C)

    // in_progress
    val SignalAmber = Color(0xFFC2641B)
    val SignalAmberDark = Color(0xFFFFB77C)
    val SignalAmberBg = Color(0xFFFBE3C8)
    val SignalAmberBgDark = Color(0xFF4A2C12)

    // pending
    val PendingTeal = Color(0xFF4C7A72)
    val PendingTealDark = Color(0xFF8FC0B7)
    val PendingBg = Color(0xFFE4ECE9)
    val PendingBgDark = Color(0xFF1E2E2B)
}

/** Typography — system sans, roman only, weight-driven hierarchy (DESIGN.md FORM).
 *  The display scale is large and tight so the day numeral reads as the
 *  typographic anchor of the schedule rail. */
val ClamitTypography = run {
    val base = Typography()
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontSize = 68.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-2.5).sp, lineHeight = 72.sp
        ),
        displayMedium = base.displayMedium.copy(
            fontSize = 52.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1.5).sp, lineHeight = 56.sp
        ),
        displaySmall = base.displaySmall.copy(
            fontSize = 40.sp, fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp, lineHeight = 46.sp
        ),
        headlineLarge = base.headlineLarge.copy(
            fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp
        ),
        headlineMedium = base.headlineMedium.copy(
            fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp
        ),
        headlineSmall = base.headlineSmall.copy(
            fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp
        ),
        titleLarge = base.titleLarge.copy(
            fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp
        ),
        titleMedium = base.titleMedium.copy(
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.15.sp, lineHeight = 24.sp
        ),
        titleSmall = base.titleSmall.copy(
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp, lineHeight = 20.sp
        ),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = base.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = base.labelLarge.copy(
            fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp
        ),
        labelMedium = base.labelMedium.copy(
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp, lineHeight = 16.sp
        ),
        labelSmall = base.labelSmall.copy(
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp, lineHeight = 14.sp
        )
    )
}

/** Shapes — one rounded-corner family with expressive radii. Cards and sheets
 *  share large, soft corners so stops on the rail feel connected. */
val ClamitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(40.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ClamitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ClamitShapes,
        typography = ClamitTypography,
        content = content
    )
}
