package com.clamit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ============================================================================
// clamit · Material 3 Expressive
// Android 12+ resolves dynamic color from the wallpaper; older devices fall
// back to the expressive light scheme (alpha25 has no expressive dark scheme,
// so dark uses the standard darkColorScheme). typography/shapes/motionScheme
// are left unset so MaterialExpressiveTheme defaults (expressive type, shape
// system, MotionScheme) apply.
//
// Status colors (DESIGN.md rule 2: status dot + label) are product semantics
// with no expressive-scheme equivalent, so they live in ClamitStatusColors —
// fixed light values, dark variants kept for callers that opt in.
// ============================================================================

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
        content = content
    )
}
